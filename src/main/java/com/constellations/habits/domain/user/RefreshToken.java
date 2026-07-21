package com.constellations.habits.domain.user;

import com.constellations.habits.domain.ValidationException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Credencial de larga vida que permite renovar el token de acceso sin volver a pedir la
 * contrasena.
 *
 * <p>Es un valor <strong>opaco y aleatorio</strong>, no un JWT. Esa decision tiene dos
 * consecuencias buenas: no puede colarse como token de acceso (el filtro lo rechaza
 * porque ni siquiera es un JWT valido), y al no llevar informacion dentro, revocarlo
 * consiste sencillamente en borrar la fila.
 *
 * <p>De la fila solo se guarda el <em>hash</em> del valor, igual que con las contrasenas:
 * quien lea la base de datos no puede reconstruir sesiones ajenas.
 */
public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt) {

    /** 256 bits de entropia: adivinarlo no es una via de ataque realista. */
    public static final int VALUE_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public RefreshToken {
        ValidationException.requirePresent(id, "id");
        ValidationException.requirePresent(userId, "userId");
        tokenHash = ValidationException.requireText(tokenHash, "tokenHash", 128);
        ValidationException.requirePresent(issuedAt, "issuedAt");
        ValidationException.requirePresent(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new ValidationException("expiresAt debe ser posterior a issuedAt");
        }
    }

    /** El valor en claro. Se entrega al cliente una sola vez y no vuelve a existir. */
    public static String generateValue() {
        byte[] bytes = new byte[VALUE_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    public static RefreshToken issue(UUID userId, String tokenHash, Instant now, Duration ttl) {
        return new RefreshToken(
                UUID.randomUUID(), userId, tokenHash, now, now.plus(ttl), null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean hasExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return !isRevoked() && !hasExpired(now);
    }

    public RefreshToken revoke(Instant now) {
        return isRevoked() ? this : new RefreshToken(id, userId, tokenHash, issuedAt, expiresAt, now);
    }

    /** El hash nunca deberia acabar en un log. */
    @Override
    public String toString() {
        return "RefreshToken[id=%s, userId=%s, expiresAt=%s, revoked=%s]"
                .formatted(id, userId, expiresAt, isRevoked());
    }
}
