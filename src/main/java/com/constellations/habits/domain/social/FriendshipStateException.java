package com.constellations.habits.domain.social;

import com.constellations.habits.domain.DomainException;

/** La transicion pedida no es valida para el estado actual de la relacion. */
public class FriendshipStateException extends DomainException {

    public FriendshipStateException(String message) {
        super(message);
    }
}
