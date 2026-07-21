package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.social.Friendship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository {

    Friendship save(Friendship friendship);

    Optional<Friendship> findById(UUID id);

    /** La relacion entre dos usuarios, exista en el sentido que exista. */
    Optional<Friendship> findBetween(UUID userA, UUID userB);

    /** Amistades aceptadas en las que participa el usuario, sea quien sea el solicitante. */
    List<Friendship> findAcceptedFor(UUID userId);

    /** Solicitudes que el usuario ha recibido y aun no ha respondido. */
    List<Friendship> findPendingReceivedBy(UUID userId);

    /** Solicitudes que el usuario ha enviado y siguen sin respuesta. */
    List<Friendship> findPendingSentBy(UUID userId);

    void delete(UUID friendshipId);
}
