package com.constellations.habits.domain.habit;

import com.constellations.habits.domain.DomainException;

import java.util.UUID;

public class ArchivedHabitException extends DomainException {

    public ArchivedHabitException(UUID habitId) {
        super("El habito " + habitId + " esta archivado y no admite cambios");
    }
}
