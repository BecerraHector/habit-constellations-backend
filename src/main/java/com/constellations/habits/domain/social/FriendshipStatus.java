package com.constellations.habits.domain.social;

public enum FriendshipStatus {

    /** Enviada por el solicitante, pendiente de que el destinatario responda. */
    PENDING,

    ACCEPTED,

    /**
     * Rechazada. La fila se conserva en vez de borrarse para que el solicitante no pueda
     * reenviar la peticion en bucle: volver a invitar exige que el destinatario retire el
     * rechazo eliminando la relacion.
     */
    DECLINED
}
