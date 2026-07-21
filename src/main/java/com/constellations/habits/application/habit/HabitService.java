package com.constellations.habits.application.habit;

import com.constellations.habits.application.exception.HabitNotFoundException;
import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.habit.CompletionWindow;
import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.habit.HabitLog;
import com.constellations.habits.domain.habit.InvalidLogDateException;
import com.constellations.habits.domain.streak.HabitProgress;
import com.constellations.habits.domain.streak.StreakCalculator;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso sobre los habitos del propio usuario.
 *
 * <p>Todas las operaciones reciben el id del autor y comprueban la propiedad: un habito
 * ajeno se reporta como inexistente, nunca como prohibido.
 */
public class HabitService {

    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final UserRepository users;
    private final Clock clock;

    public HabitService(
            HabitRepository habits, HabitLogRepository logs, UserRepository users, Clock clock) {
        this.habits = habits;
        this.logs = logs;
        this.users = users;
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

    public HabitView rename(UUID ownerId, UUID habitId, CreateHabitCommand command) {
        Habit habit = requireOwned(ownerId, habitId);
        Habit renamed = habits.save(habit.rename(command.name(), command.description()));
        return new HabitView(renamed, progressOf(renamed, todayFor(ownerId)));
    }

    public void archive(UUID ownerId, UUID habitId) {
        Habit habit = requireOwned(ownerId, habitId);
        habits.save(habit.archive(clock.instant()));
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

        if (!logs.existsByHabitAndDate(habitId, logDate)) {
            logs.save(HabitLog.of(habitId, logDate, clock.instant()));
        }
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
