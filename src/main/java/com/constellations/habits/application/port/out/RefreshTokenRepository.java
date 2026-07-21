package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.user.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    /** Se busca por hash porque el valor en claro no se guarda en ningun sitio. */
    Optional<RefreshToken> findByHash(String tokenHash);

    /**
     * Revoca todas las sesiones vivas de un usuario. Es la reaccion a detectar que un
     * token ya usado vuelve a presentarse: si eso pasa, alguien tiene una copia.
     */
    int revokeAllForUser(UUID userId, Instant now);

    /** Al darse de baja no hay nada que auditar: las sesiones se borran, no se marcan. */
    void deleteAllForUser(UUID userId);
}
