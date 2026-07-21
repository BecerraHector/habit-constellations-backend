package com.constellations.habits.infrastructure.security;

import com.constellations.habits.application.port.out.AccessTokenIssuer;
import com.constellations.habits.domain.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Emite y valida los JWT de acceso. */
@Service
public class JwtService implements AccessTokenIssuer {

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = buildKey(properties.secret());
    }

    @Override
    public IssuedToken issue(User user) {
        var now = clock.instant();
        Duration ttl = properties.accessTokenTtl();

        String token = Jwts.builder()
                .subject(user.id().toString())
                .issuer(properties.issuer())
                .claim("email", user.email())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();

        return new IssuedToken(token, ttl);
    }

    /**
     * @return el id del usuario si el token es autentico y no ha expirado; vacio en
     *     cualquier otro caso. Un token invalido no es una excepcion: es simplemente
     *     una peticion no autenticada.
     */
    public Optional<UUID> resolveUserId(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("""
                    Falta la clave de firmado JWT.
                    Definela antes de arrancar, por ejemplo:
                      export HABITS_SECURITY_JWT_SECRET="$(openssl rand -base64 48)"
                    """);
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < JwtProperties.MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "HABITS_SECURITY_JWT_SECRET debe tener al menos "
                            + JwtProperties.MIN_SECRET_BYTES + " bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
