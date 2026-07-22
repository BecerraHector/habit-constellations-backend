package com.constellations.habits.domain.streak;

import com.constellations.habits.domain.galaxy.Luminosity;

import java.time.LocalDate;

/**
 * Un dia del mapa personal: cuantos habitos existian y cuantos se cumplieron.
 *
 * <p>Usa la misma escala {@link Luminosity} que el mapa de una galaxia a proposito: el
 * usuario ya aprendio que ese degradado significa "proporcion cumplida" y que el maximo
 * se reserva al pleno. Dos rampas distintas para la misma idea serian ruido.
 *
 * @param activeHabits cuantos habitos contaban <em>ese</em> dia, no hoy
 * @param completions  cuantos de ellos se cumplieron
 * @param level        intensidad segun {@link Luminosity}
 */
public record SkyDay(LocalDate date, int activeHabits, int completions, int level) {

    public static SkyDay of(LocalDate date, int completions, int activeHabits) {
        return new SkyDay(
                date, activeHabits, completions, Luminosity.levelOf(completions, activeHabits));
    }

    /** Se cumplio todo lo que existia ese dia. */
    public boolean isPerfect() {
        return activeHabits > 0 && level == Luminosity.MAX_LEVEL;
    }
}
