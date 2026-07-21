package com.constellations.habits.application.exception;

public class EmailAlreadyUsedException extends ApplicationException {

    public EmailAlreadyUsedException() {
        super("Ya existe una cuenta con ese email");
    }
}
