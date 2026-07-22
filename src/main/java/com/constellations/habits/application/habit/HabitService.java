package com.constellations.habits.application.habit;

import com.constellations.habits.application.exception.HabitNotFoundException;
import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.application.galaxy.GalaxyService;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.TransactionRunner;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.habit.CompletionWindow;
import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.habit.HabitLog;
import com.constellations.habits.domain.habit.InvalidLogDateException;
import com.constellations.habits.domain.streak.HabitProgress;
import com.constellations.habits.domain.streak.HabitSpan;
import com.constellations.habits.domain.streak.SkyCalculator;
import com.constellations.habits.domain.streak.StreakCalculator;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Casos de uso sobre los habitos del propio usuario.
 *
 * <p>Todas las operaciones reciben el id del autor y comprueban la propiedad: un habito
 * ajeno se reporta como inexistente, nunca como prohibido.
 */
public class HabitService {

    /** Ventana de historial cuando el cliente no pide un tramo concreto. */
    static final int DEFAULT_HISTORY_DAYS = 90;

    /** Tope de la ventana: nadie necesita releer mas de un ano de golpe. */
    static final int MAX_HISTORY_DAYS = 366;

    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final UserRepository users;
    private final GalaxyService galaxies;
    private final TransactionRunner transaction;
    private final Clock clock;

    public HabitService(
            HabitRepository habits,
            HabitLogRepository logs,
            UserRepository users,
            GalaxyService galaxies,
            TransactionRunner transaction,
            Clock clock) {
        this.habits = habits;
        this.logs = logs;
        this.users = users;
        this.galaxies = galaxies;
        this.transaction = transaction;
        this.clock = clock;
    }

    public HabitView create(UUID ownerId, CreateHabitCommand command) {
        Habit habit = habits.save(
                Habit.create(ownerId, command.name(), command.description(), clock.instant()));
        return new HabitView(habit, HabitProgress.EMPTY);
    }

    public List<HabitView> listActive(UUID ownerId) {
        LocalDate today = todayFor(ownerId);
        List<Habit> active = habits.findActiveByOwner(ownerId);
        if (active.isEmpty()) {
            return List.of();
        }

        // Una sola consulta para todos los logs, no una por habito.
        Map<UUID, List<LocalDate>> datesByHabit =
                logs.findDatesByHabits(active.stream().map(Habit::id).toList());

        return active.stream()
                .map(habit -> new HabitView(
                        habit,
                        StreakCalculator.calculate(
                                datesByHabit.getOrDefault(habit.id(), List.of()), today)))
                .toList();
    }

    public HabitView get(UUID ownerId, UUID habitId) {
        Habit habit = requireOwned(ownerId, habitId);
        return new HabitView(habit, progressOf(habit, todayFor(ownerId)));
    }

    /**
     * Las fechas cumplidas dentro de una ventana, para pintar el calendario del habito.
     *
     * <p>La normalizacion es silenciosa, como la ventana del mapa de brillo: un valor
     * ausente o invalido cae al defecto en vez de fallar. El final se recorta al hoy del
     * usuario (el futuro no puede contener logs) y el tamano se acota para que ningun
     * cliente relea historias enteras de golpe.
     */
    public HabitHistory history(UUID ownerId, UUID habitId, LocalDate from, LocalDate to) {
        Habit habit = requireOwned(ownerId, habitId);
        Window window = Window.resolve(from, to, todayFor(ownerId));

        List<LocalDate> dates = logs
                .findDatesByHabitsBetween(List.of(habit.id()), window.from(), window.to())
                .getOrDefault(habit.id(), List.of())
                .stream()
                .sorted()
                .toList();
        return new HabitHistory(window.from(), window.to(), dates);
    }

    /**
     * El mapa del cielo propio: todos los habitos a la vez, dia a dia. El denominador de
     * cada dia lo reconstruye {@link SkyCalculator} con las vidas de los habitos,
     * archivados incluidos — por eso aqui se cargan todos, no solo los activos.
     */
    public SkyView sky(UUID ownerId, LocalDate from, LocalDate to) {
        User user = users.findById(ownerId).orElseThrow(() -> new UserNotFoundException(ownerId));
        Window window = Window.resolve(from, to, user.today(clock.instant()));

        List<Habit> all = habits.findAllByOwner(ownerId);
        if (all.isEmpty()) {
            return new SkyView(window.from(), window.to(), List.of());
        }

        List<HabitSpan> spans = all.stream()
                .map(habit -> new HabitSpan(
                        habit.id(),
                        LocalDate.ofInstant(habit.createdAt(), user.zoneId()),
                        habit.archivedAt() == null
                                ? null
                                : LocalDate.ofInstant(habit.archivedAt(), user.zoneId())))
                .toList();

        Map<UUID, Set<LocalDate>> completions = logs
                .findDatesByHabitsBetween(all.stream().map(Habit::id).toList(), window.from(), window.to())
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));

        return new SkyView(
                window.from(),
                window.to(),
                SkyCalculator.map(spans, completions, window.from(), window.to()));
    }

    /**
     * Ventana de consulta normalizada en silencio: un valor ausente o invalido cae al
     * defecto en vez de fallar, el final se recorta al hoy del usuario (el futuro no
     * puede contener logs) y el tamano se acota a un ano.
     */
    private record Window(LocalDate from, LocalDate to) {

        static Window resolve(LocalDate from, LocalDate to, LocalDate today) {
            LocalDate end = (to == null || to.isAfter(today)) ? today : to;
            LocalDate start = (from == null || from.isAfter(end))
                    ? end.minusDays(DEFAULT_HISTORY_DAYS - 1)
                    : from;
            LocalDate floor = end.minusDays(MAX_HISTORY_DAYS - 1);
            return new Window(start.isBefore(floor) ? floor : start, end);
        }
    }

    public HabitView rename(UUID ownerId, UUID habitId, CreateHabitCommand command) {
        Habit habit = requireOwned(ownerId, habitId);
        Habit renamed = habits.save(habit.rename(command.name(), command.description()));
        return new HabitView(renamed, progressOf(renamed, todayFor(ownerId)));
    }

    /**
     * Archivar el habito lo saca ademas de las galaxias que alimentaba. Si no, su dueno
     * seguiria contando en el denominador del brillo sin poder ya marcar nada, y
     * oscureceria al grupo indefinidamente.
     */
    public void archive(UUID ownerId, UUID habitId) {
        Habit habit = requireOwned(ownerId, habitId);
        LocalDate today = todayFor(ownerId);

        // Ambas cosas o ninguna: un habito archivado cuyas pertenencias siguieran vivas
        // seguiria contando en el denominador del brillo sin poder marcar nada.
        transaction.run(() -> {
            habits.save(habit.archive(clock.instant()));
            galaxies.releaseArchivedHabit(habitId, today);
        });
    }

    /**
     * Marca el habito como cumplido. Idempotente: repetir la llamada sobre un dia ya
     * marcado devuelve el mismo estado en vez de fallar, porque un doble tap del cliente
     * no deberia ser un error.
     */
    public HabitView complete(UUID ownerId, UUID habitId, LocalDate requestedDate) {
        Habit habit = requireOwned(ownerId, habitId);
        if (habit.isArchived()) {
            throw new InvalidLogDateException("No se puede registrar en un habito archivado");
        }

        LocalDate today = todayFor(ownerId);
        LocalDate logDate = requestedDate != null ? requestedDate : today;
        CompletionWindow.requireWithinWindow(logDate, today);

        logs.saveIfAbsent(HabitLog.of(habitId, logDate, clock.instant()));
        return new HabitView(habit, progressOf(habit, today));
    }

    /** Deshace un cumplimiento. Solo dentro de la misma ventana en que se pudo crear. */
    public HabitView uncomplete(UUID ownerId, UUID habitId, LocalDate requestedDate) {
        Habit habit = requireOwned(ownerId, habitId);

        LocalDate today = todayFor(ownerId);
        LocalDate logDate = requestedDate != null ? requestedDate : today;
        CompletionWindow.requireWithinWindow(logDate, today);

        logs.deleteByHabitAndDate(habitId, logDate);
        return new HabitView(habit, progressOf(habit, today));
    }

    private HabitProgress progressOf(Habit habit, LocalDate today) {
        return StreakCalculator.calculate(logs.findDatesByHabit(habit.id()), today);
    }

    private Habit requireOwned(UUID ownerId, UUID habitId) {
        return habits.findById(habitId)
                .filter(habit -> habit.isOwnedBy(ownerId))
                .orElseThrow(() -> new HabitNotFoundException(habitId));
    }

    /** El "hoy" que cuenta es el del usuario, no el del servidor. */
    private LocalDate todayFor(UUID ownerId) {
        User user = users.findById(ownerId).orElseThrow(() -> new UserNotFoundException(ownerId));
        return user.today(clock.instant());
    }
}
