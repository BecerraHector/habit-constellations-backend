package com.constellations.habits.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SpringDataHabitRepository extends JpaRepository<HabitEntity, UUID> {

    List<HabitEntity> findByOwnerIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID ownerId);

    List<HabitEntity> findByOwnerIdInAndArchivedAtIsNull(Collection<UUID> ownerIds);

    List<HabitEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
