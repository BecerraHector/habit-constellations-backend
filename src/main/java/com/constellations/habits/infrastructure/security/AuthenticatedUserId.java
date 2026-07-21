package com.constellations.habits.infrastructure.security;

import java.util.UUID;

/** Principal minimo: lo unico que los casos de uso necesitan saber de quien llama. */
public record AuthenticatedUserId(UUID value) {}
