package com.constellations.habits.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;

interface SpringDataGalaxyRepository extends JpaRepository<GalaxyEntity, UUID> {

    /**
     * Galaxias ordenadas por miembros activos. El recuento se hace en SQL y no en
     * memoria: ordenar por popularidad exige conocer el total de todas ellas, y traerlas
     * enteras para contarlas seria traer la tabla.
     */
    @Query("""
            SELECT g, COUNT(m.id) AS members
            FROM GalaxyEntity g
            LEFT JOIN GalaxyMembershipEntity m ON m.galaxyId = g.id AND m.leftOn IS NULL
            WHERE :theme IS NULL OR g.theme = :theme
            GROUP BY g
            ORDER BY members DESC, g.createdAt DESC
            """)
    List<GalaxyEntity> findMostPopular(@Param("theme") String theme, Pageable page);

    @Query("""
            SELECT g.theme, COUNT(DISTINCT g.id), COUNT(m.id)
            FROM GalaxyEntity g
            LEFT JOIN GalaxyMembershipEntity m ON m.galaxyId = g.id AND m.leftOn IS NULL
            GROUP BY g.theme
            ORDER BY COUNT(m.id) DESC, COUNT(DISTINCT g.id) DESC
            """)
    List<Object[]> countByTheme(Pageable page);
}
