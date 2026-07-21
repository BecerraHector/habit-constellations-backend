package com.constellations.habits.application.social;

import java.time.Instant;
import java.util.UUID;

/**
 * Una solicitud pendiente vista desde uno de los dos lados.
 *
 * <p>No expone el codigo de invitacion del otro usuario: quien recibe una solicitud no
 * tiene por que quedarse con la credencial de quien se la envio.
 */
public record FriendRequestView(
        UUID requestId,
        UUID otherUserId,
        String otherUserDisplayName,
        Direction direction,
        Instant createdAt) {

    public enum Direction {
        /** La recibio el usuario y le toca responder. */
        INCOMING,
        /** La envio el usuario y espera respuesta. */
        OUTGOING
    }
}
