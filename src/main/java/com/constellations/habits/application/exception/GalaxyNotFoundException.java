package com.constellations.habits.application.exception;

import java.util.UUID;

public class GalaxyNotFoundException extends ApplicationException {

    public GalaxyNotFoundException(UUID galaxyId) {
        super("No existe la galaxia " + galaxyId);
    }
}
