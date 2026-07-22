package com.constellations.habits.application.habit;

import com.constellations.habits.domain.streak.SkyDay;

import java.time.LocalDate;
import java.util.List;

/**
 * El mapa del cielo propio dentro de una ventana ya normalizada. Como en el historial,
 * la ventana efectiva viaja con los datos: el servicio pudo recortar la pedida.
 */
public record SkyView(LocalDate from, LocalDate to, List<SkyDay> days) {}
