package com.constellations.habits.application.exception;

/** Fallo de un caso de uso que no es una violacion de invariantes del dominio. */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }
}
