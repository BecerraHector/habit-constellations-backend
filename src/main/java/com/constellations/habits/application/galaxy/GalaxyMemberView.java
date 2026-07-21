package com.constellations.habits.application.galaxy;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Un miembro visto desde dentro de la galaxia.
 *
 * <p>Solo el nombre visible. Ni email ni codigo de invitacion, que siguen siendo
 * credenciales, ni rachas: repetir aqui una cifra que ya vive en el panel personal
 * ensuciaria la pantalla y ademas dejaria a un desconocido perfilar la constancia
 * general de alguien a quien solo comparte un habito.
 */
public record GalaxyMemberView(UUID userId, String displayName, LocalDate joinedOn) {}
