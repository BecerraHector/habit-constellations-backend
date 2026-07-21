package com.constellations.habits.domain.habit;

import com.constellations.habits.domain.DomainException;

public class InvalidLogDateException extends DomainException {

    public InvalidLogDateException(String message) {
        super(message);
    }
}
