package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.domain.habit.HabitLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class JpaHabitLogRepository implements HabitLogRepository {

    private final SpringDataHabitLogRepository delegate;

    JpaHabitLogRepository(SpringDataHabitLogRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public HabitLog save(HabitLog log) {
        return toDomain(delegate.save(toEntity(log)));
    }

    @Override
    public List<LocalDate> findDatesByHabit(UUID habitId) {
        return delegate.findLogDates(habitId);
    }

    @Override
    public Map<UUID, List<LocalDate>> findDatesByHabits(List<UUID> habitIds) {
        if (habitIds.isEmpty()) {
            return Map.of();
        }
        return delegate.findLogDatesForHabits(habitIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (UUID) row[0],
                        Collectors.mapping(row -> (LocalDate) row[1], Collectors.toList())));
    }

    @Override
    public boolean existsByHabitAndDate(UUID habitId, LocalDate logDate) {
        return delegate.existsByHabitIdAndLogDate(habitId, logDate);
    }

    @Override
    @Transactional
    public boolean deleteByHabitAndDate(UUID habitId, LocalDate logDate) {
        return delegate.deleteByHabitIdAndLogDate(habitId, logDate) > 0;
    }

    private static HabitLogEntity toEntity(HabitLog log) {
        var entity = new HabitLogEntity();
        entity.setId(log.id());
        entity.setHabitId(log.habitId());
        entity.setLogDate(log.logDate());
        entity.setCompletedAt(log.completedAt());
        return entity;
    }

    private static HabitLog toDomain(HabitLogEntity entity) {
        return new HabitLog(
                entity.getId(), entity.getHabitId(), entity.getLogDate(), entity.getCompletedAt());
    }
}
