package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.user.User;

import java.util.Collection;
import java.util.List;
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

    List<User> findAllById(Collection<UUID> ids);

    /** El codigo llega ya normalizado por {@link com.constellations.habits.domain.user.InviteCode}. */
    Optional<User> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
