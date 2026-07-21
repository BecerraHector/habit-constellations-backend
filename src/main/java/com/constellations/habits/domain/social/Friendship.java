package com.constellations.habits.domain.social;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.util.UUID;

/**
 * Relacion entre dos usuarios. Nace como solicitud y solo es amistad cuando el
 * destinatario acepta: nadie observa el progreso de otro sin haberlo consentido.
 *
 * <p>La direccion (quien pidio a quien) se conserva incluso tras aceptar, porque es la
 * que decide quien puede responder. La relacion en si es simetrica: una vez aceptada,
 * ambos se ven por igual.
 */
public record Friendship(
        UUID id,
        UUID requesterId,
        UUID addresseeId,
        FriendshipStatus status,
        Instant createdAt,
        Instant respondedAt) {

    public Friendship {
        ValidationException.requirePresent(id, "id");
        ValidationException.requirePresent(requesterId, "requesterId");
        ValidationException.requirePresent(addresseeId, "addresseeId");
        ValidationException.requirePresent(status, "status");
        ValidationException.requirePresent(createdAt, "createdAt");

        if (requesterId.equals(addresseeId)) {
            throw new SelfFriendshipException();
        }
    }

    public static Friendship request(UUID requesterId, UUID addresseeId, Instant now) {
        return new Friendship(
                UUID.randomUUID(), requesterId, addresseeId, FriendshipStatus.PENDING, now, null);
    }

    public Friendship accept(UUID actingUserId, Instant now) {
        requirePendingDecisionBy(actingUserId);
        return new Friendship(
                id, requesterId, addresseeId, FriendshipStatus.ACCEPTED, createdAt, now);
    }

    public Friendship decline(UUID actingUserId, Instant now) {
        requirePendingDecisionBy(actingUserId);
        return new Friendship(
                id, requesterId, addresseeId, FriendshipStatus.DECLINED, createdAt, now);
    }

    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == FriendshipStatus.PENDING;
    }

    public boolean involves(UUID userId) {
        return requesterId.equals(userId) || addresseeId.equals(userId);
    }

    /** El otro extremo de la relacion visto desde {@code userId}. */
    public UUID otherParty(UUID userId) {
        if (requesterId.equals(userId)) {
            return addresseeId;
        }
        if (addresseeId.equals(userId)) {
            return requesterId;
        }
        throw new ValidationException("El usuario no participa en esta relacion");
    }

    /**
     * Solo el destinatario decide, y solo mientras siga pendiente. Que el solicitante
     * pudiera aceptar su propia peticion vaciaria de sentido el consentimiento.
     */
    private void requirePendingDecisionBy(UUID actingUserId) {
        if (!addresseeId.equals(actingUserId)) {
            throw new FriendshipStateException(
                    "Solo el destinatario de la solicitud puede responderla");
        }
        if (status != FriendshipStatus.PENDING) {
            throw new FriendshipStateException("La solicitud ya fue respondida");
        }
    }
}
