package com.constellations.habits.application.galaxy;

import java.util.UUID;

/**
 * @param habitId habito personal ya existente que el creador quiere enlazar. Si viene
 *                vacio se le crea uno nuevo con el nombre de la galaxia, para que en su
 *                panel personal siga habiendo un unico habito y una unica racha.
 */
public record CreateGalaxyCommand(String name, String description, String theme, UUID habitId) {}
