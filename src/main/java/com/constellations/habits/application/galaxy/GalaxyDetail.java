package com.constellations.habits.application.galaxy;

import com.constellations.habits.domain.galaxy.GalaxyMap;

/**
 * La galaxia con su mapa de brillo.
 *
 * <p>Sin la lista de miembros a proposito: en una galaxia abierta puede tener cientos de
 * nombres que ni caben en pantalla ni hacen falta para pintar el mapa. Se pide aparte y
 * paginada.
 */
public record GalaxyDetail(GalaxyView galaxy, GalaxyMap map) {}
