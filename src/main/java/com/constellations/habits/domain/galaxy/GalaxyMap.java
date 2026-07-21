package com.constellations.habits.domain.galaxy;

import java.time.LocalDate;
import java.util.List;

/**
 * Ventana de dias con su brillo, para pintarla como un mapa de calor.
 *
 * <p>Es un valor derivado, igual que {@code HabitProgress}: se recalcula desde los logs
 * en cada consulta y no se persiste, de modo que no puede desincronizarse.
 */
public record GalaxyMap(LocalDate from, LocalDate to, List<GalaxyDay> days) {

    public static final GalaxyMap EMPTY = new GalaxyMap(null, null, List.of());

    public GalaxyMap {
        days = List.copyOf(days);
    }

    /** Dias en que cumplio el grupo entero. */
    public int perfectDays() {
        return (int) days.stream().filter(GalaxyDay::isPerfect).count();
    }

    /** Estrellas encendidas en la ventana, sumando a todos los miembros. */
    public int totalStars() {
        return days.stream().mapToInt(GalaxyDay::completions).sum();
    }

    /**
     * Brillo medio de la ventana, entre 0 y 1. Es la cifra que resume "como va el grupo"
     * sin necesidad de hablar de rachas.
     */
    public double averageRatio() {
        return days.isEmpty() ? 0d : days.stream().mapToDouble(GalaxyDay::ratio).average().orElse(0d);
    }
}
