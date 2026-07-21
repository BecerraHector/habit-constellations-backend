package com.constellations.habits.application.exception;

import java.util.UUID;

/**
 * Se lanza tanto si el habito no existe como si es de otro usuario: desde fuera
 * ambos casos deben ser indistinguibles para no filtrar que ids estan en uso.
 */
public class HabitNotFoundException extends ApplicationException {

    public HabitNotFoundException(UUID habitId) {
        super("No se encontro el habito " + habitId);
    }
}
