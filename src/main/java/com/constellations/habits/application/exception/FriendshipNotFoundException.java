package com.constellations.habits.application.exception;

/**
 * Se lanza tanto si la relacion no existe como si el usuario no participa en ella:
 * distinguirlas revelaria que identificadores estan en uso.
 */
public class FriendshipNotFoundException extends ApplicationException {

    public FriendshipNotFoundException() {
        super("No se encontro la solicitud o amistad");
    }
}
