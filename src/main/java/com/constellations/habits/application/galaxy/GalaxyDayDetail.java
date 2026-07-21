package com.constellations.habits.application.galaxy;

import java.time.LocalDate;
import java.util.List;

/**
 * El desglose de un dia concreto, para cuando se toca una estrella del mapa.
 *
 * <p>Se nombra a quienes cumplieron y no a quienes faltaron. La informacion deducible es
 * la misma, pero el encuadre no: una lista de ausentes convierte el mapa en un tablon de
 * reproches, y la gente abandona las apps que la senalan.
 */
public record GalaxyDayDetail(
        LocalDate date,
        int activeMembers,
        int completions,
        int level,
        List<String> completedBy) {}
