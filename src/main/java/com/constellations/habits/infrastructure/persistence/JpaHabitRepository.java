package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.domain.habit.Habit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaHabitRepository implements HabitRepository {

    private final SpringDataHabitRepository delegate;

    JpaHabitRepository(SpringDataHabitRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Habit save(Habit habit) {
        return toDomain(delegate.save(toEntity(habit)));
    }

    @Override
    public Optional<Habit> findById(UUID id) {
        return delegate.findById(id).map(JpaHabitRepository::toDomain);
    }

    @Override
    public List<Habit> findActiveByOwner(UUID ownerId) {
        return delegate.findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(ownerId).stream()
                .map(JpaHabitRepository::toDomain)
                .toList();
    }

    @Override
    public List<Habit> findActiveByOwners(Collection<UUID> ownerIds) {
        if (ownerIds.isEmpty()) {
            return List.of();
        }
        return delegate.findByOwnerIdInAndArchivedAtIsNull(ownerIds).stream()
                .map(JpaHabitRepository::toDomain)
                .toList();
    }

    @Override
    public List<Habit> findAllByOwner(UUID ownerId) {
        return delegate.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(JpaHabitRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAll(Collection<UUID> habitIds) {
        if (habitIds.isEmpty()) {
            return;
        }
        // Los logs caen por la clave foranea ON DELETE CASCADE de la migracion V1.
        delegate.deleteAllByIdInBatch(habitIds);
    }

    private static HabitEntity toEntity(Habit habit) {
        var entity = new HabitEntity();
        entity.setId(habit.id());
        entity.setOwnerId(habit.ownerId());
        entity.setName(habit.name());
        entity.setDescription(habit.description());
        entity.setCreatedAt(habit.createdAt());
        entity.setArchivedAt(habit.archivedAt());
        return entity;
    }

    private static Habit toDomain(HabitEntity entity) {
        return new Habit(
                entity.getId(),
                entity.getOwnerId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getArchivedAt());
    }
}
