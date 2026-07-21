package com.constellations.habits.application.galaxy;

import com.constellations.habits.domain.galaxy.GalaxyMap;

import java.util.List;

/** La galaxia con su mapa de brillo y quienes la habitan ahora mismo. */
public record GalaxyDetail(GalaxyView galaxy, GalaxyMap map, List<GalaxyMemberView> members) {}
