package com.constellations.habits.domain.user;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cuenta de un usuario.
 *
 * <p>La zona horaria no es un detalle de presentacion: define donde cae la medianoche
 * del usuario y por tanto que dia se le atribuye a cada cumplimiento. Vive en el dominio.
 */
public record User(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        ZoneId zoneId,
        Instant createdAt) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");

    public User {
        ValidationException.requirePresent(id, "id");
        email = normalizeEmail(email);
        ValidationException.requireText(passwordHash, "passwordHash", 255);
        displayName = ValidationException.requireText(displayName, "displayName", 60);
        ValidationException.requirePresent(zoneId, "zoneId");
        ValidationException.requirePresent(createdAt, "createdAt");
    }

    public static User register(
            String email, String passwordHash, String displayName, ZoneId zoneId, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, displayName, zoneId, now);
    }

    /** El "hoy" del usuario, que puede no coincidir con el del servidor. */
    public LocalDate today(Instant now) {
        return LocalDate.ofInstant(now, zoneId);
    }

    public static String normalizeEmail(String raw) {
        String value = ValidationException.requireText(raw, "email", 255).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(value).matches()) {
            throw new ValidationException("email no tiene un formato valido");
        }
        return value;
    }

    /** Evita que el hash de la contrasena acabe en un log por accidente. */
    @Override
    public String toString() {
        return "User[id=%s, email=%s, displayName=%s, zoneId=%s]"
                .formatted(id, email, displayName, zoneId);
    }
}
