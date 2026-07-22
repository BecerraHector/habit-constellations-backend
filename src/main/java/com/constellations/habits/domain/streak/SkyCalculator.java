package com.constellations.habits.domain.streak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El mapa del cielo propio: para cada dia de la ventana, cuantos habitos existian y
 * cuantos se cumplieron, condensado en un nivel de brillo.
 *
 * <p>Funcion pura como {@link StreakCalculator}: recibe las vidas de los habitos y sus
 * fechas cumplidas, no lee relojes ni repositorios. El denominador de cada dia son los
 * habitos que existian <em>ese</em> dia — un habito archivado ayer no oscurece el mes
 * pasado, y uno recien creado no convierte el ano anterior en fallos.
 */
public final class SkyCalculator {

    private SkyCalculator() {}

    /**
     * @param habits            las vidas de todos los habitos del usuario, archivados
     *                          incluidos: sin ellos no se reconstruye el denominador pasado
     * @param completionsByHabit fechas cumplidas de cada habito dentro de la ventana
     */
    public static List<SkyDay> map(
            Collection<HabitSpan> habits,
            Map<UUID, Set<LocalDate>> completionsByHabit,
            LocalDate from,
            LocalDate to) {

        if (from == null || to == null || to.isBefore(from)) {
            return List.of();
        }

        return from.datesUntil(to.plusDays(1))
                .map(day -> dayOf(day, habits, completionsByHabit))
                .toList();
    }

    private static SkyDay dayOf(
            LocalDate day,
            Collection<HabitSpan> habits,
            Map<UUID, Set<LocalDate>> completionsByHabit) {

        int active = 0;
        int completed = 0;

        for (HabitSpan span : habits) {
            boolean completedThatDay = completionsByHabit
                    .getOrDefault(span.habitId(), Set.of())
                    .contains(day);

            if (!span.countsOn(day, completedThatDay)) {
                continue;
            }

            active++;
            if (completedThatDay) {
                completed++;
            }
        }

        return SkyDay.of(day, completed, active);
    }
}
