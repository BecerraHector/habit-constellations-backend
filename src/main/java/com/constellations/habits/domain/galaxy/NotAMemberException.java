package com.constellations.habits.domain.galaxy;

import com.constellations.habits.domain.DomainException;

/** Se intento operar sobre una galaxia a la que no se pertenece. */
public class NotAMemberException extends DomainException {

    public NotAMemberException(String message) {
        super(message);
    }
}
