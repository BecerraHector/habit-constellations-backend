package com.constellations.habits.application.user;

import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.user.InviteCode;

/**
 * Entrega codigos de invitacion que no esten ya en uso.
 *
 * <p>Vive aparte porque lo necesitan dos casos de uso: el registro y la regeneracion del
 * codigo. Con 32^8 combinaciones una colision es rarisima, pero existe un indice unico en
 * base de datos y el reintento evita que un choque fortuito rompa un alta.
 */
public class InviteCodeAllocator {

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository users;

    public InviteCodeAllocator(UserRepository users) {
        this.users = users;
    }

    public InviteCode allocate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            InviteCode candidate = InviteCode.generate();
            if (!users.existsByInviteCode(candidate.value())) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "No se pudo generar un codigo de invitacion libre tras " + MAX_ATTEMPTS
                        + " intentos");
    }
}
