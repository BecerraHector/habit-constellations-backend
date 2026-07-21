package com.constellations.habits.application.port.out;

import com.constellations.habits.application.galaxy.ThemeCount;
import com.constellations.habits.domain.galaxy.Galaxy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalaxyRepository {

    Galaxy save(Galaxy galaxy);

    Optional<Galaxy> findById(UUID id);

    List<Galaxy> findAllById(Collection<UUID> ids);

    /**
     * Galaxias abiertas a las que unirse, de la mas concurrida a la menos.
     *
     * <p>Ordenar por miembros activos es una decision de producto: una galaxia con gente
     * dentro tiene mapa que mirar, y una recien creada esta a oscuras.
     *
     * @param theme filtro opcional; {@code null} devuelve las de cualquier tema
     */
    List<Galaxy> findMostPopular(String theme, int limit);

    /** Temas existentes ordenados por miembros activos, para el catalogo. */
    List<ThemeCount> countByTheme(int limit);
}
