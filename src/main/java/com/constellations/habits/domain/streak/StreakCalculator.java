package com.constellations.habits.domain.streak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Motor de rachas. Funcion pura: mismos logs y mismo "hoy", mismo resultado.
 *
 * <p>Reglas vigentes:
 * <ul>
 *   <li>Un dia sin cumplir corta la racha. No hay dias de gracia ni congelaciones.</li>
 *   <li>La racha sigue viva si el ultimo cumplimiento fue hoy <em>o ayer</em>: mientras
 *       el dia en curso no termine, el usuario aun puede sostenerla.</li>
 *   <li>Cada {@value #CYCLE_LENGTH} dias consecutivos cierran una constelacion.</li>
 * </ul>
 *
 * <p>El "hoy" se recibe como parametro en vez de leerse del reloj: cada usuario tiene su
 * propia zona horaria, y ademas asi el motor es testeable sin manipular el tiempo.
 */
public final class StreakCalculator {

    /** Dias consecutivos que componen una constelacion completa. */
    public static final int CYCLE_LENGTH = 30;

    private StreakCalculator() {}

    public static HabitProgress calculate(Collection<LocalDate> completions, LocalDate today) {
        if (completions == null || completions.isEmpty()) {
            return HabitProgress.EMPTY;
        }

        // TreeSet: los logs pueden llegar en cualquier orden y con duplicados logicos.
        TreeSet<LocalDate> days = new TreeSet<>(completions);

        int longest = 0;
        int constellations = 0;
        int runLength = 0;
        LocalDate previous = null;

        for (LocalDate day : days) {
            runLength = (previous != null && day.equals(previous.plusDays(1))) ? runLength + 1 : 1;
            longest = Math.max(longest, runLength);
            // Una racha de 65 dias son dos constelaciones cerradas y 5 estrellas sueltas.
            if (runLength % CYCLE_LENGTH == 0) {
                constellations++;
            }
            previous = day;
        }

        LocalDate lastCompleted = days.last();
        boolean completedToday = days.contains(today);
        int current = currentStreak(days, today, completedToday);

        return new HabitProgress(
                current,
                longest,
                days.size(),
                lastCompleted,
                completedToday,
                current % CYCLE_LENGTH,
                constellations);
    }

    private static int currentStreak(TreeSet<LocalDate> days, LocalDate today, boolean completedToday) {
        LocalDate cursor;
        if (completedToday) {
            cursor = today;
        } else if (days.contains(today.minusDays(1))) {
            // Aun no ha marcado hoy, pero el dia no ha terminado: la racha sigue viva.
            cursor = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
