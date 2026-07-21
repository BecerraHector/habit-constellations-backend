package com.constellations.habits.application.galaxy;

/**
 * Una entrada del catalogo. La popularidad sale de los datos reales, no de una lista
 * fija: lo que se sugiere es lo que la gente esta sosteniendo de verdad.
 *
 * @param members miembros activos sumando todas las galaxias del tema
 */
public record ThemeCount(String theme, int galaxies, int members) {

    public static ThemeCount empty(String theme) {
        return new ThemeCount(theme, 0, 0);
    }
}
