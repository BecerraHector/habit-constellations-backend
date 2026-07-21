package com.constellations.habits.application.social;

import com.constellations.habits.application.Page;
import com.constellations.habits.application.PageQuery;
import com.constellations.habits.application.exception.FriendshipAlreadyExistsException;
import com.constellations.habits.application.exception.FriendshipNotFoundException;
import com.constellations.habits.application.exception.InviteCodeNotFoundException;
import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.application.port.out.FriendshipRepository;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.application.user.InviteCodeAllocator;
import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.social.Friendship;
import com.constellations.habits.domain.social.FriendshipStatus;
import com.constellations.habits.domain.streak.HabitProgress;
import com.constellations.habits.domain.streak.StreakCalculator;
import com.constellations.habits.domain.user.InviteCode;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Casos de uso de la capa social: invitaciones, solicitudes y panel de amigos. */
public class FriendshipService {

    private final FriendshipRepository friendships;
    private final UserRepository users;
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final InviteCodeAllocator inviteCodes;
    private final Clock clock;

    public FriendshipService(
            FriendshipRepository friendships,
            UserRepository users,
            HabitRepository habits,
            HabitLogRepository logs,
            InviteCodeAllocator inviteCodes,
            Clock clock) {
        this.friendships = friendships;
        this.users = users;
        this.habits = habits;
        this.logs = logs;
        this.inviteCodes = inviteCodes;
        this.clock = clock;
    }

    public User regenerateInviteCode(UUID userId) {
        User user = requireUser(userId);
        return users.save(user.withInviteCode(inviteCodes.allocate()));
    }

    /**
     * Envia una solicitud al dueno de un codigo. No confirma ni desmiente si el codigo
     * pertenece a alguien concreto mas alla de lo que ya implica el resultado.
     */
    public FriendRequestView sendRequest(UUID requesterId, String rawInviteCode) {
        String code = InviteCode.normalize(rawInviteCode);

        User addressee = users.findByInviteCode(code).orElseThrow(InviteCodeNotFoundException::new);
        // Usar el propio codigo cae aqui y lo rechaza el invariante de Friendship.
        Friendship friendship = Friendship.request(requesterId, addressee.id(), clock.instant());

        friendships.findBetween(requesterId, addressee.id()).ifPresent(existing -> {
            throw new FriendshipAlreadyExistsException(switch (existing.status()) {
                case ACCEPTED -> "Ya sois amigos";
                case PENDING -> "Ya hay una solicitud pendiente entre vosotros";
                case DECLINED -> "Esa solicitud fue rechazada previamente";
            });
        });

        return toRequestView(friendships.save(friendship), requesterId, addressee.displayName());
    }

    public List<FriendRequestView> listIncomingRequests(UUID userId) {
        return toRequestViews(friendships.findPendingReceivedBy(userId), userId);
    }

    public List<FriendRequestView> listOutgoingRequests(UUID userId) {
        return toRequestViews(friendships.findPendingSentBy(userId), userId);
    }

    public void acceptRequest(UUID userId, UUID requestId) {
        Friendship request = requireParticipant(userId, requestId);
        friendships.save(request.accept(userId, clock.instant()));
    }

    public void declineRequest(UUID userId, UUID requestId) {
        Friendship request = requireParticipant(userId, requestId);
        friendships.save(request.decline(userId, clock.instant()));
    }

    /**
     * Elimina la relacion por completo, en cualquier estado. Borrar en vez de marcar deja
     * la puerta abierta a volver a invitarse mas adelante.
     */
    public void removeFriend(UUID userId, UUID otherUserId) {
        Friendship friendship = friendships.findBetween(userId, otherUserId)
                .orElseThrow(FriendshipNotFoundException::new);
        friendships.delete(friendship.id());
    }

    /**
     * Panel social. Resuelve amigos, habitos y logs en tres consultas en total, sea cual
     * sea el numero de amigos.
     */
    public Page<FriendSummary> listFriends(UUID userId, PageQuery query) {
        Page<Friendship> accepted = friendships.findAcceptedFor(userId, query);
        if (accepted.content().isEmpty()) {
            return Page.empty(query);
        }

        Map<UUID, Friendship> byFriendId = accepted.content().stream()
                .collect(Collectors.toMap(f -> f.otherParty(userId), f -> f));

        List<Habit> friendHabits = habits.findActiveByOwners(byFriendId.keySet());
        Map<UUID, List<LocalDate>> datesByHabit =
                logs.findDatesByHabits(friendHabits.stream().map(Habit::id).toList());
        Map<UUID, List<Habit>> habitsByOwner =
                friendHabits.stream().collect(Collectors.groupingBy(Habit::ownerId));

        List<FriendSummary> summaries = users.findAllById(byFriendId.keySet()).stream()
                .map(friend -> summarize(
                        friend,
                        byFriendId.get(friend.id()),
                        habitsByOwner.getOrDefault(friend.id(), List.of()),
                        datesByHabit))
                .sorted(Comparator.comparing(FriendSummary::bestCurrentStreak).reversed()
                        .thenComparing(FriendSummary::displayName))
                .toList();

        // El orden por racha se aplica dentro del tramo, no sobre el total: la racha es
        // un valor calculado que no existe en la base de datos y no se puede ordenar por
        // el en SQL. Es una limitacion conocida, no un descuido.
        return new Page<>(
                summaries, accepted.page(), accepted.size(), accepted.totalElements());
    }

    private FriendSummary summarize(
            User friend,
            Friendship friendship,
            List<Habit> friendHabits,
            Map<UUID, List<LocalDate>> datesByHabit) {

        // Cada amigo se evalua en su propia zona horaria: su "hoy" no tiene por que
        // coincidir con el de quien mira el panel.
        LocalDate theirToday = friend.today(clock.instant());

        int bestCurrent = 0;
        int longestEver = 0;
        int stars = 0;
        int constellations = 0;
        int completedToday = 0;

        for (Habit habit : friendHabits) {
            HabitProgress progress = StreakCalculator.calculate(
                    datesByHabit.getOrDefault(habit.id(), List.of()), theirToday);

            bestCurrent = Math.max(bestCurrent, progress.currentStreak());
            longestEver = Math.max(longestEver, progress.longestStreak());
            stars += progress.totalCompletions();
            constellations += progress.completedConstellations();
            if (progress.completedToday()) {
                completedToday++;
            }
        }

        return new FriendSummary(
                friend.id(),
                friend.displayName(),
                friendship.respondedAt(),
                friendHabits.size(),
                bestCurrent,
                longestEver,
                stars,
                constellations,
                completedToday);
    }

    private List<FriendRequestView> toRequestViews(List<Friendship> requests, UUID userId) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> names = users
                .findAllById(requests.stream().map(f -> f.otherParty(userId)).toList()).stream()
                .collect(Collectors.toMap(User::id, User::displayName));

        return requests.stream()
                .map(request -> toRequestView(
                        request, userId, names.get(request.otherParty(userId))))
                .toList();
    }

    private static FriendRequestView toRequestView(
            Friendship friendship, UUID viewerId, String otherDisplayName) {

        var direction = friendship.requesterId().equals(viewerId)
                ? FriendRequestView.Direction.OUTGOING
                : FriendRequestView.Direction.INCOMING;

        return new FriendRequestView(
                friendship.id(),
                friendship.otherParty(viewerId),
                otherDisplayName,
                direction,
                friendship.createdAt());
    }

    /** Una relacion ajena se reporta como inexistente, igual que un habito ajeno. */
    private Friendship requireParticipant(UUID userId, UUID friendshipId) {
        return friendships.findById(friendshipId)
                .filter(friendship -> friendship.involves(userId))
                .orElseThrow(FriendshipNotFoundException::new);
    }

    private User requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    /** Expuesto para que otros casos de uso puedan filtrar por amistad aceptada. */
    public boolean areFriends(UUID userA, UUID userB) {
        return friendships.findBetween(userA, userB)
                .filter(friendship -> friendship.status() == FriendshipStatus.ACCEPTED)
                .isPresent();
    }
}
