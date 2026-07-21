package com.constellations.habits.domain.habit;

import java.time.LocalDate;

/**
 * Ventana en la que se admite registrar un cumplimiento.
 *
 * <p>Regla del producto: se puede marcar hoy o ayer (para cubrir el olvido de anoche),
 * nunca el futuro ni nada mas antiguo. Sin este limite la racha dejaria de significar
 * constancia real, porque cualquiera podria rellenar el pasado a voluntad.
 */
public final class CompletionWindow {

    /** Dias hacia atras, ademas de hoy, que siguen siendo editables. */
    public static final int BACKFILL_DAYS = 1;

    private CompletionWindow() {}

    public static void requireWithinWindow(LocalDate logDate, LocalDate today) {
        if (logDate.isAfter(today)) {
            throw new InvalidLogDateException("No se puede registrar un habito en una fecha futura");
        }
        if (logDate.isBefore(today.minusDays(BACKFILL_DAYS))) {
            throw new InvalidLogDateException(
                    "Solo se puede registrar hoy o hasta " + BACKFILL_DAYS + " dia(s) atras");
        }
    }

    public static boolean isWithinWindow(LocalDate logDate, LocalDate today) {
        return !logDate.isAfter(today) && !logDate.isBefore(today.minusDays(BACKFILL_DAYS));
    }
}
