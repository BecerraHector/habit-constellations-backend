package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.habit.Habit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitRepository {

    Habit save(Habit habit);

    Optional<Habit> findById(UUID id);

    /** Habitos no archivados del usuario, del mas reciente al mas antiguo. */
    List<Habit> findActiveByOwner(UUID ownerId);

    List<Habit> findAllByOwner(UUID ownerId);
}
