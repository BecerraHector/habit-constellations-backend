package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.user.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida hacia el almacen de usuarios. Lo implementa la capa de
 * infraestructura; ni el dominio ni los casos de uso saben que hay detras.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    /** El email llega ya normalizado a minusculas por {@link User#normalizeEmail}. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
