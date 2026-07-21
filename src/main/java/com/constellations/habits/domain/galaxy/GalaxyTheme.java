package com.constellations.habits.domain.galaxy;

import com.constellations.habits.domain.ValidationException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Etiqueta tematica de una galaxia ("gym", "estudio", "dormir-7-8-horas").
 *
 * <p>Se normaliza a un slug porque el catalogo de temas populares se construye contando
 * galaxias agrupadas por este valor: sin normalizar, "Gym", "gym " y "GYM" serian tres
 * filas distintas y ninguna pareceria popular.
 */
public record GalaxyTheme(String value) {

    public static final int MAX_LENGTH = 32;

    /**
     * Semillas del catalogo, para que no aparezca vacio con la base recien creada. Son
     * sugerencias: cualquiera puede escribir un tema que no este en la lista.
     */
    public static final List<GalaxyTheme> SUGGESTED = List.of(
            new GalaxyTheme("gym"),
            new GalaxyTheme("estudio"),
            new GalaxyTheme("dormir-7-8-horas"),
            new GalaxyTheme("lectura"),
            new GalaxyTheme("meditacion"),
            new GalaxyTheme("correr"));

    public GalaxyTheme {
        value = normalize(value);
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("theme no puede estar vacio");
        }
        // Se descartan las tildes a proposito: "meditacion" y "meditación" deben caer en
        // el mismo tema, o el catalogo se parte en dos entradas que son la misma cosa.
        String slug = Normalizer
                .normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[\\s_]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.isEmpty()) {
            throw new ValidationException("theme debe contener alguna letra o numero");
        }
        if (slug.length() > MAX_LENGTH) {
            throw new ValidationException("theme no puede superar " + MAX_LENGTH + " caracteres");
        }
        return slug;
    }
}
