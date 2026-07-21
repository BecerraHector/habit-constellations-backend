package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.galaxy.ThemeCount;
import com.constellations.habits.application.port.out.GalaxyRepository;
import com.constellations.habits.domain.galaxy.Galaxy;
import com.constellations.habits.domain.galaxy.GalaxyTheme;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaGalaxyRepository implements GalaxyRepository {

    private final SpringDataGalaxyRepository delegate;

    JpaGalaxyRepository(SpringDataGalaxyRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Galaxy save(Galaxy galaxy) {
        return toDomain(delegate.save(toEntity(galaxy)));
    }

    @Override
    public Optional<Galaxy> findById(UUID id) {
        return delegate.findById(id).map(JpaGalaxyRepository::toDomain);
    }

    @Override
    public List<Galaxy> findAllById(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return delegate.findAllById(ids).stream().map(JpaGalaxyRepository::toDomain).toList();
    }

    @Override
    public List<Galaxy> findMostPopular(String theme, int limit) {
        return delegate.findMostPopular(theme, PageRequest.of(0, limit)).stream()
                .map(JpaGalaxyRepository::toDomain)
                .toList();
    }

    @Override
    public List<ThemeCount> countByTheme(int limit) {
        return delegate.countByTheme(PageRequest.of(0, limit)).stream()
                .map(row -> new ThemeCount(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()))
                .toList();
    }

    private static GalaxyEntity toEntity(Galaxy galaxy) {
        var entity = new GalaxyEntity();
        entity.setId(galaxy.id());
        entity.setName(galaxy.name());
        entity.setDescription(galaxy.description());
        entity.setTheme(galaxy.theme().value());
        entity.setCreatorId(galaxy.creatorId());
        entity.setCreatedAt(galaxy.createdAt());
        return entity;
    }

    private static Galaxy toDomain(GalaxyEntity entity) {
        return new Galaxy(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                new GalaxyTheme(entity.getTheme()),
                entity.getCreatorId(),
                entity.getCreatedAt());
    }
}
