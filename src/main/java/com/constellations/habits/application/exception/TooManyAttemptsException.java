package com.constellations.habits.application.exception;

import java.time.Duration;

/** Demasiados intentos fallidos seguidos; hay que esperar antes de volver a probar. */
public class TooManyAttemptsException extends ApplicationException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super("Demasiados intentos. Vuelve a probar en unos segundos");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
