package com.constellations.habits.application.port.out;

import com.constellations.habits.application.Page;
import com.constellations.habits.application.PageQuery;
import com.constellations.habits.domain.galaxy.GalaxyMembership;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface GalaxyMembershipRepository {

    GalaxyMembership save(GalaxyMembership membership);

    Optional<GalaxyMembership> findActive(UUID galaxyId, UUID userId);

    /**
     * Todas las filas de la galaxia, <strong>incluidas las de quienes ya se fueron</strong>:
     * el mapa necesita saber cuantos miembros habia cada dia para calcular el brillo.
     */
    List<GalaxyMembership> findAllByGalaxy(UUID galaxyId);

    /** Miembros vivos por tramos, para no volcar cientos de nombres de una vez. */
    Page<GalaxyMembership> findActiveByGalaxy(UUID galaxyId, PageQuery query);

    List<GalaxyMembership> findActiveByUser(UUID userId);

    /**
     * Historial completo del usuario, incluidas las galaxias que ya abandono. Al darse de
     * baja hace falta para saber que habitos suyos alimentaron alguna vez un mapa y por
     * tanto no se pueden borrar.
     */
    List<GalaxyMembership> findAllByUser(UUID userId);

    /** Version por lotes para no contar miembros galaxia a galaxia en los listados. */
    Map<UUID, Integer> countActiveByGalaxies(Collection<UUID> galaxyIds);

    /**
     * Pertenencias vivas alimentadas por un habito. Al archivarlo hay que cerrarlas: si
     * no, su dueno seguiria pesando en el denominador sin poder ya marcar nada.
     */
    List<GalaxyMembership> findActiveByHabit(UUID habitId);
}
