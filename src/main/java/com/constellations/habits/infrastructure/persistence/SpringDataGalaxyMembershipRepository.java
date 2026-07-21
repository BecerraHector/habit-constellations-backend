package com.constellations.habits.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataGalaxyMembershipRepository
        extends JpaRepository<GalaxyMembershipEntity, UUID> {

    Optional<GalaxyMembershipEntity> findByGalaxyIdAndUserIdAndLeftOnIsNull(
            UUID galaxyId, UUID userId);

    /** Historial completo: el mapa necesita tambien a quienes ya se fueron. */
    List<GalaxyMembershipEntity> findByGalaxyId(UUID galaxyId);

    org.springframework.data.domain.Page<GalaxyMembershipEntity>
            findByGalaxyIdAndLeftOnIsNullOrderByJoinedOnAsc(
                    UUID galaxyId, org.springframework.data.domain.Pageable pageable);

    List<GalaxyMembershipEntity> findByUserIdAndLeftOnIsNull(UUID userId);

    List<GalaxyMembershipEntity> findByHabitIdAndLeftOnIsNull(UUID habitId);

    List<GalaxyMembershipEntity> findByUserId(UUID userId);

    @Query("""
            SELECT m.galaxyId, COUNT(m.id)
            FROM GalaxyMembershipEntity m
            WHERE m.galaxyId IN :galaxyIds AND m.leftOn IS NULL
            GROUP BY m.galaxyId
            """)
    List<Object[]> countActiveByGalaxies(@Param("galaxyIds") Collection<UUID> galaxyIds);
}
