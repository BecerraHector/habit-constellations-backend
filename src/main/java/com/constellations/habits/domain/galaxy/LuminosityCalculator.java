package com.constellations.habits.domain.galaxy;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Motor de brillo. Funcion pura, como {@code StreakCalculator}: mismas pertenencias y
 * mismos logs, mismo mapa.
 *
 * <p>La regla que la hace no trivial es el denominador. Cada dia se divide entre quienes
 * pertenecian a la galaxia <em>ese</em> dia, no entre los de hoy. Si se usara el recuento
 * actual, entrar en un grupo oscureceria retroactivamente meses en los que nadie hizo
 * nada distinto, y el mapa dejaria de ser un registro de lo que paso.
 */
public final class LuminosityCalculator {

    private LuminosityCalculator() {}

    /**
     * @param memberships todas las filas de la galaxia, incluidas las de quienes ya se
     *                    fueron: sin ellas no se puede reconstruir el denominador pasado
     * @param completionsByHabit fechas cumplidas de cada habito enlazado
     */
    public static GalaxyMap map(
            Collection<GalaxyMembership> memberships,
            Map<UUID, Set<LocalDate>> completionsByHabit,
            LocalDate from,
            LocalDate to) {

        if (from == null || to == null || to.isBefore(from)) {
            return GalaxyMap.EMPTY;
        }

        List<GalaxyDay> days = from.datesUntil(to.plusDays(1))
                .map(day -> dayOf(day, memberships, completionsByHabit))
                .toList();

        return new GalaxyMap(from, to, days);
    }

    private static GalaxyDay dayOf(
            LocalDate day,
            Collection<GalaxyMembership> memberships,
            Map<UUID, Set<LocalDate>> completionsByHabit) {

        int active = 0;
        int completed = 0;

        for (GalaxyMembership membership : memberships) {
            if (!membership.isActiveOn(day)) {
                continue;
            }
            active++;
            // Solo cuentan los cumplimientos de quien era miembro: si alguien se fue y
            // siguio con el habito por su cuenta, sus estrellas son suyas, no del grupo.
            if (completionsByHabit
                    .getOrDefault(membership.habitId(), Set.of())
                    .contains(day)) {
                completed++;
            }
        }

        return GalaxyDay.of(day, completed, active);
    }
}
