package com.constellations.habits.application.galaxy;

import com.constellations.habits.application.exception.AlreadyMemberException;
import com.constellations.habits.application.exception.GalaxyNotFoundException;
import com.constellations.habits.application.exception.HabitNotFoundException;
import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.application.port.out.GalaxyMembershipRepository;
import com.constellations.habits.application.port.out.GalaxyRepository;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.galaxy.Galaxy;
import com.constellations.habits.domain.galaxy.GalaxyMap;
import com.constellations.habits.domain.galaxy.GalaxyMembership;
import com.constellations.habits.domain.galaxy.GalaxyTheme;
import com.constellations.habits.domain.galaxy.Luminosity;
import com.constellations.habits.domain.galaxy.LuminosityCalculator;
import com.constellations.habits.domain.galaxy.NotAMemberException;
import com.constellations.habits.domain.habit.ArchivedHabitException;
import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.streak.StreakCalculator;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Casos de uso de las constelaciones compartidas.
 *
 * <p>Unirse a una galaxia no crea un seguimiento paralelo: enlaza un habito personal, o
 * lo crea si no existia. Un unico registro alimenta a la vez la racha propia y el brillo
 * del grupo, de modo que no puede darse el caso de ver dos rachas distintas del mismo
 * habito segun la pantalla en que se mire.
 */
public class GalaxyService {

    /** Ventana por defecto del mapa: el mismo ciclo que cierra una constelacion. */
    public static final int DEFAULT_WINDOW_DAYS = StreakCalculator.CYCLE_LENGTH;

    public static final int MAX_WINDOW_DAYS = 365;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final GalaxyRepository galaxies;
    private final GalaxyMembershipRepository memberships;
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final UserRepository users;
    private final Clock clock;

    public GalaxyService(
            GalaxyRepository galaxies,
            GalaxyMembershipRepository memberships,
            HabitRepository habits,
            HabitLogRepository logs,
            UserRepository users,
            Clock clock) {
        this.galaxies = galaxies;
        this.memberships = memberships;
        this.habits = habits;
        this.logs = logs;
        this.users = users;
        this.clock = clock;
    }

    /** Crear una galaxia mete dentro al creador: una galaxia vacia no tiene sentido. */
    public GalaxyView create(UUID userId, CreateGalaxyCommand command) {
        User user = requireUser(userId);
        Galaxy galaxy = galaxies.save(Galaxy.create(
                userId, command.name(), command.description(), command.theme(), clock.instant()));

        GalaxyMembership mine =
                enroll(user, galaxy, command.habitId(), user.today(clock.instant()));
        return GalaxyView.of(galaxy, 1, mine);
    }

    public GalaxyView join(UUID userId, UUID galaxyId, UUID habitId) {
        User user = requireUser(userId);
        Galaxy galaxy = requireGalaxy(galaxyId);

        memberships.findActive(galaxyId, userId).ifPresent(existing -> {
            throw new AlreadyMemberException();
        });

        GalaxyMembership mine = enroll(user, galaxy, habitId, user.today(clock.instant()));
        return GalaxyView.of(galaxy, activeMemberCount(galaxyId), mine);
    }

    /**
     * Salir no borra la pertenencia ni el habito. La fila se conserva porque el mapa
     * pasado se apoya en ella, y el habito sigue siendo del usuario: lo empezo el, y las
     * estrellas que lleve ganadas no son del grupo.
     */
    public void leave(UUID userId, UUID galaxyId) {
        User user = requireUser(userId);
        GalaxyMembership mine = memberships.findActive(galaxyId, userId)
                .orElseThrow(() -> new NotAMemberException("No perteneces a esta galaxia"));

        memberships.save(mine.leave(user.today(clock.instant()), clock.instant()));
    }

    /**
     * Cierra las pertenencias que alimentaba un habito recien archivado. Sin esto su
     * dueno seguiria contando en el denominador sin poder marcar nada, y oscureceria al
     * grupo indefinidamente.
     */
    public void releaseArchivedHabit(UUID habitId, LocalDate today) {
        Instant now = clock.instant();
        memberships.findActiveByHabit(habitId)
                .forEach(membership -> memberships.save(membership.leave(today, now)));
    }

    public List<GalaxyView> listMine(UUID userId) {
        List<GalaxyMembership> mine = memberships.findActiveByUser(userId);
        if (mine.isEmpty()) {
            return List.of();
        }

        Map<UUID, GalaxyMembership> byGalaxy = mine.stream()
                .collect(Collectors.toMap(GalaxyMembership::galaxyId, Function.identity()));
        Map<UUID, Integer> counts = memberships.countActiveByGalaxies(byGalaxy.keySet());

        return galaxies.findAllById(byGalaxy.keySet()).stream()
                .map(galaxy -> GalaxyView.of(
                        galaxy, counts.getOrDefault(galaxy.id(), 0), byGalaxy.get(galaxy.id())))
                .sorted(Comparator.comparing(GalaxyView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Galaxias abiertas a las que unirse, opcionalmente filtradas por tema. */
    public List<GalaxyView> discover(UUID userId, String rawTheme, Integer limit) {
        String theme = (rawTheme == null || rawTheme.isBlank())
                ? null
                : GalaxyTheme.normalize(rawTheme);

        List<Galaxy> found = galaxies.findMostPopular(theme, cap(limit));
        if (found.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = found.stream().map(Galaxy::id).toList();
        Map<UUID, Integer> counts = memberships.countActiveByGalaxies(ids);
        Map<UUID, GalaxyMembership> mine = memberships.findActiveByUser(userId).stream()
                .collect(Collectors.toMap(GalaxyMembership::galaxyId, Function.identity()));

        return found.stream()
                .map(galaxy -> GalaxyView.of(
                        galaxy, counts.getOrDefault(galaxy.id(), 0), mine.get(galaxy.id())))
                .toList();
    }

    /**
     * Catalogo de temas, de mas a menos concurrido. Las sugerencias que todavia nadie ha
     * estrenado se anaden al final con cero: sirven de punto de partida sin fingir una
     * popularidad que no tienen.
     */
    public List<ThemeCount> catalog(Integer limit) {
        Map<String, ThemeCount> merged = new LinkedHashMap<>();
        galaxies.countByTheme(cap(limit)).forEach(entry -> merged.put(entry.theme(), entry));

        GalaxyTheme.SUGGESTED.forEach(
                suggested -> merged.putIfAbsent(suggested.value(), ThemeCount.empty(suggested.value())));

        return List.copyOf(merged.values());
    }

    public GalaxyDetail get(UUID userId, UUID galaxyId, Integer windowDays) {
        User user = requireUser(userId);
        Galaxy galaxy = requireGalaxy(galaxyId);

        List<GalaxyMembership> all = memberships.findAllByGalaxy(galaxyId);
        LocalDate today = user.today(clock.instant());
        LocalDate from = windowStart(all, today, windowDays);

        GalaxyMap map = LuminosityCalculator.map(all, completionsOf(all), from, today);

        List<GalaxyMembership> active = all.stream().filter(GalaxyMembership::isActive).toList();
        GalaxyMembership mine = active.stream()
                .filter(membership -> membership.userId().equals(userId))
                .findFirst()
                .orElse(null);

        return new GalaxyDetail(
                GalaxyView.of(galaxy, active.size(), mine), map, memberViews(active));
    }

    /** Desglose de un dia concreto: quienes lo iluminaron. */
    public GalaxyDayDetail dayDetail(UUID galaxyId, LocalDate date) {
        requireGalaxy(galaxyId);

        List<GalaxyMembership> onThatDay = memberships.findAllByGalaxy(galaxyId).stream()
                .filter(membership -> membership.isActiveOn(date))
                .toList();

        Map<UUID, Set<LocalDate>> completions = completionsOf(onThatDay);
        List<GalaxyMembership> completed = onThatDay.stream()
                .filter(membership -> completions
                        .getOrDefault(membership.habitId(), Set.of())
                        .contains(date))
                .toList();

        List<String> names = memberViews(completed).stream()
                .map(GalaxyMemberView::displayName)
                .toList();

        return new GalaxyDayDetail(
                date,
                onThatDay.size(),
                completed.size(),
                Luminosity.levelOf(completed.size(), onThatDay.size()),
                names);
    }

    private GalaxyMembership enroll(
            User user, Galaxy galaxy, UUID habitId, LocalDate today) {

        UUID linkedHabit = habitId != null
                ? requireLinkableHabit(user.id(), habitId).id()
                : habits.save(Habit.create(
                        user.id(), galaxy.name(), galaxy.description(), clock.instant())).id();

        return memberships.save(
                GalaxyMembership.join(galaxy.id(), user.id(), linkedHabit, today, clock.instant()));
    }

    /**
     * El mapa nunca empieza antes de que existiera el primer miembro: pintar dias en los
     * que la galaxia estaba vacia solo produce una franja apagada que no significa nada.
     */
    private LocalDate windowStart(
            List<GalaxyMembership> history, LocalDate today, Integer windowDays) {

        int days = windowDays == null || windowDays <= 0
                ? DEFAULT_WINDOW_DAYS
                : Math.min(windowDays, MAX_WINDOW_DAYS);

        LocalDate requested = today.minusDays(days - 1L);
        return history.stream()
                .map(GalaxyMembership::joinedOn)
                .min(Comparator.naturalOrder())
                .filter(requested::isBefore)
                .orElse(requested);
    }

    /** Una sola consulta de logs para toda la galaxia, sea cual sea el numero de miembros. */
    private Map<UUID, Set<LocalDate>> completionsOf(List<GalaxyMembership> involved) {
        List<UUID> habitIds = involved.stream()
                .map(GalaxyMembership::habitId)
                .distinct()
                .toList();
        if (habitIds.isEmpty()) {
            return Map.of();
        }

        return logs.findDatesByHabits(habitIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private List<GalaxyMemberView> memberViews(List<GalaxyMembership> shown) {
        if (shown.isEmpty()) {
            return List.of();
        }

        Map<UUID, GalaxyMembership> byUser = shown.stream()
                .collect(Collectors.toMap(GalaxyMembership::userId, Function.identity()));

        return users.findAllById(byUser.keySet()).stream()
                .map(user -> new GalaxyMemberView(
                        user.id(), user.displayName(), byUser.get(user.id()).joinedOn()))
                .sorted(Comparator.comparing(
                        GalaxyMemberView::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Habit requireLinkableHabit(UUID ownerId, UUID habitId) {
        Habit habit = habits.findById(habitId)
                .filter(candidate -> candidate.isOwnedBy(ownerId))
                .orElseThrow(() -> new HabitNotFoundException(habitId));

        if (habit.isArchived()) {
            throw new ArchivedHabitException(habitId);
        }
        return habit;
    }

    private int activeMemberCount(UUID galaxyId) {
        return memberships.countActiveByGalaxies(List.of(galaxyId)).getOrDefault(galaxyId, 0);
    }

    private Galaxy requireGalaxy(UUID galaxyId) {
        return galaxies.findById(galaxyId).orElseThrow(() -> new GalaxyNotFoundException(galaxyId));
    }

    private User requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private static int cap(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
