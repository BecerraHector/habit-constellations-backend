package com.constellations.habits.application.exception;

import java.util.UUID;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(UUID userId) {
        super("No se encontro el usuario " + userId);
    }
}
