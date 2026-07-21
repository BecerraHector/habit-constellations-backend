package com.constellations.habits.application.social;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que un usuario ve de su amigo.
 *
 * <p>Solo agregados: deliberadamente no incluye los nombres de los habitos. "Terapia" o
 * "dejar de fumar" no deberian filtrarse por el hecho de aceptar una solicitud, y la
 * motivacion social se sostiene igual con las cifras.
 *
 * @param activeHabits         cuantos habitos lleva, sin decir cuales
 * @param bestCurrentStreak    su mejor racha viva ahora mismo
 * @param longestStreakEver    su mejor racha historica
 * @param totalStars           dias cumplidos sumando todos sus habitos
 * @param totalConstellations  constelaciones cerradas en total
 * @param completedToday       habitos que ya ha marcado hoy, en su propia zona horaria
 */
public record FriendSummary(
        UUID userId,
        String displayName,
        Instant friendsSince,
        int activeHabits,
        int bestCurrentStreak,
        int longestStreakEver,
        int totalStars,
        int totalConstellations,
        int completedToday) {}
