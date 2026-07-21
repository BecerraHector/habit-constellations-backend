package com.constellations.habits.domain.galaxy;

import java.time.LocalDate;

/**
 * Una estrella del mapa compartido.
 *
 * @param activeMembers cuantos pertenecian a la galaxia <em>ese</em> dia, no hoy
 * @param completions   cuantos de ellos cumplieron
 * @param level         intensidad segun {@link Luminosity}
 */
public record GalaxyDay(LocalDate date, int activeMembers, int completions, int level) {

    public static GalaxyDay of(LocalDate date, int completions, int activeMembers) {
        return new GalaxyDay(
                date, activeMembers, completions, Luminosity.levelOf(completions, activeMembers));
    }

    /** Cumplio todo el grupo. */
    public boolean isPerfect() {
        return level == Luminosity.MAX_LEVEL;
    }

    /** Proporcion cumplida, entre 0 y 1. Un dia sin miembros vale 0, no indefinido. */
    public double ratio() {
        return activeMembers <= 0 ? 0d : (double) completions / activeMembers;
    }
}
