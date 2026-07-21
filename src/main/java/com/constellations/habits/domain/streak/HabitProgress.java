package com.constellations.habits.domain.streak;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Estado calculado de un habito a una fecha dada. Es un valor derivado de los logs:
 * no se persiste, se recalcula, para que nunca pueda quedar desincronizado.
 *
 * @param currentStreak          dias consecutivos vivos ahora mismo
 * @param longestStreak          mejor racha historica, aunque ya se haya roto
 * @param totalCompletions       dias distintos cumplidos en toda la vida del habito
 * @param lastCompletedDate      ultimo dia cumplido, si hay alguno
 * @param completedToday         si el dia en curso ya esta marcado
 * @param starsInCurrentCycle    estrellas dibujadas dentro del ciclo actual
 * @param completedConstellations constelaciones cerradas a lo largo de la historia
 */
public record HabitProgress(
        int currentStreak,
        int longestStreak,
        int totalCompletions,
        LocalDate lastCompletedDate,
        boolean completedToday,
        int starsInCurrentCycle,
        int completedConstellations) {

    public static final HabitProgress EMPTY =
            new HabitProgress(0, 0, 0, null, false, 0, 0);

    public Optional<LocalDate> lastCompleted() {
        return Optional.ofNullable(lastCompletedDate);
    }

    /** Cuantos dias faltan para cerrar la constelacion en curso. */
    public int daysToNextConstellation() {
        return StreakCalculator.CYCLE_LENGTH - starsInCurrentCycle;
    }
}
