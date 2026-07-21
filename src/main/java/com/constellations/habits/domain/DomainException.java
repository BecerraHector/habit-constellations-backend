package com.constellations.habits.domain;

/** Violacion de una regla de negocio. Las capas externas la traducen a HTTP. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
