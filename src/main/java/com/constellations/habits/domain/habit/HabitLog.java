package com.constellations.habits.domain.habit;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Constancia de que un habito se cumplio un dia concreto.
 *
 * <p>{@code logDate} es la fecha <em>local del usuario</em>, no la derivada de
 * {@code completedAt}: alguien en Lima que marca a las 23:00 debe registrar ese dia,
 * no el siguiente en UTC.
 */
public record HabitLog(UUID id, UUID habitId, LocalDate logDate, Instant completedAt) {

    public HabitLog {
        ValidationException.requirePresent(id, "id");
        ValidationException.requirePresent(habitId, "habitId");
        ValidationException.requirePresent(logDate, "logDate");
        ValidationException.requirePresent(completedAt, "completedAt");
    }

    public static HabitLog of(UUID habitId, LocalDate logDate, Instant completedAt) {
        return new HabitLog(UUID.randomUUID(), habitId, logDate, completedAt);
    }
}
