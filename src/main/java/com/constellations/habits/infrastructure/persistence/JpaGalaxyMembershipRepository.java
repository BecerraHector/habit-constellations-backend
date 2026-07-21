package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.Page;
import com.constellations.habits.application.PageQuery;
import com.constellations.habits.application.port.out.GalaxyMembershipRepository;
import com.constellations.habits.domain.galaxy.GalaxyMembership;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class JpaGalaxyMembershipRepository implements GalaxyMembershipRepository {

    private final SpringDataGalaxyMembershipRepository delegate;

    JpaGalaxyMembershipRepository(SpringDataGalaxyMembershipRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public GalaxyMembership save(GalaxyMembership membership) {
        return toDomain(delegate.save(toEntity(membership)));
    }

    @Override
    public Optional<GalaxyMembership> findActive(UUID galaxyId, UUID userId) {
        return delegate.findByGalaxyIdAndUserIdAndLeftOnIsNull(galaxyId, userId)
                .map(JpaGalaxyMembershipRepository::toDomain);
    }

    @Override
    public List<GalaxyMembership> findAllByGalaxy(UUID galaxyId) {
        return map(delegate.findByGalaxyId(galaxyId));
    }

    @Override
    public Page<GalaxyMembership> findActiveByGalaxy(UUID galaxyId, PageQuery query) {
        var found = delegate.findByGalaxyIdAndLeftOnIsNullOrderByJoinedOnAsc(
                galaxyId, PageRequest.of(query.page(), query.size()));

        return new Page<>(
                map(found.getContent()), query.page(), query.size(), found.getTotalElements());
    }

    @Override
    public List<GalaxyMembership> findActiveByUser(UUID userId) {
        return map(delegate.findByUserIdAndLeftOnIsNull(userId));
    }

    @Override
    public Map<UUID, Integer> countActiveByGalaxies(Collection<UUID> galaxyIds) {
        if (galaxyIds.isEmpty()) {
            return Map.of();
        }
        return delegate.countActiveByGalaxies(galaxyIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0], row -> ((Number) row[1]).intValue()));
    }

    @Override
    public List<GalaxyMembership> findActiveByHabit(UUID habitId) {
        return map(delegate.findByHabitIdAndLeftOnIsNull(habitId));
    }

    private static List<GalaxyMembership> map(List<GalaxyMembershipEntity> entities) {
        return entities.stream().map(JpaGalaxyMembershipRepository::toDomain).toList();
    }

    private static GalaxyMembershipEntity toEntity(GalaxyMembership membership) {
        var entity = new GalaxyMembershipEntity();
        entity.setId(membership.id());
        entity.setGalaxyId(membership.galaxyId());
        entity.setUserId(membership.userId());
        entity.setHabitId(membership.habitId());
        entity.setJoinedOn(membership.joinedOn());
        entity.setJoinedAt(membership.joinedAt());
        entity.setLeftOn(membership.leftOn());
        entity.setLeftAt(membership.leftAt());
        return entity;
    }

    private static GalaxyMembership toDomain(GalaxyMembershipEntity entity) {
        return new GalaxyMembership(
                entity.getId(),
                entity.getGalaxyId(),
                entity.getUserId(),
                entity.getHabitId(),
                entity.getJoinedOn(),
                entity.getJoinedAt(),
                entity.getLeftOn(),
                entity.getLeftAt());
    }
}
