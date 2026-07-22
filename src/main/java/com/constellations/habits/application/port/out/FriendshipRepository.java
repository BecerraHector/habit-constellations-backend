package com.constellations.habits.application.port.out;

import com.constellations.habits.application.Page;
import com.constellations.habits.application.PageQuery;
import com.constellations.habits.domain.social.Friendship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository {

    Friendship save(Friendship friendship);

    Optional<Friendship> findById(UUID id);

    /** La relacion entre dos usuarios, exista en el sentido que exista. */
    Optional<Friendship> findBetween(UUID userA, UUID userB);

    /**
     * Amistades aceptadas en las que participa el usuario, sea quien sea el solicitante.
     *
     * <p>Paginado porque resolver cada amigo cuesta sus habitos y sus logs: sin tope, el
     * panel de alguien muy sociable acabaria leyendo el historial de media base de datos
     * en una sola peticion.
     */
    Page<Friendship> findAcceptedFor(UUID userId, PageQuery query);

    /**
     * Todas las amistades aceptadas de un usuario, sin paginar. Alimenta filtros (como
     * el mapa de brillo acotado a amigos), no listados: el tamano lo limita cuantas
     * amistades reales puede tener una persona, no un parametro de pagina.
     */
    List<Friendship> findAllAcceptedFor(UUID userId);

    /** Solicitudes que el usuario ha recibido y aun no ha respondido. */
    List<Friendship> findPendingReceivedBy(UUID userId);

    /** Solicitudes que el usuario ha enviado y siguen sin respuesta. */
    List<Friendship> findPendingSentBy(UUID userId);

    void delete(UUID friendshipId);

    /**
     * Borra toda relacion en la que participe el usuario. Al darse de baja no queda nada
     * que conservar: una amistad con una lapida no significa nada, y mantenerla dejaria
     * un nombre vacio en el panel del otro.
     */
    void deleteAllInvolving(UUID userId);
}
