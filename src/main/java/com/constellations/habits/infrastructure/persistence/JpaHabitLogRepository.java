package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.domain.habit.HabitLog;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Se intenta insertar y se acepta el rechazo del indice unico como "ya estaba".
     *
     * <p>Comprobar antes y escribir despues deja una ventana entre ambas consultas: dos
     * toques simultaneos —o el reintento de un cliente con mala red— pasarian los dos la
     * comprobacion y el segundo reventaria contra la restriccion. Delegar en la base de
     * datos convierte esa carrera en el resultado que ya se prometia: nada cambia y no
     * es un error.
     */
    @Override
    @Transactional
    public boolean saveIfAbsent(HabitLog log) {
        if (delegate.existsByHabitIdAndLogDate(log.habitId(), log.logDate())) {
            return false;
        }
        try {
            delegate.saveAndFlush(toEntity(log));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Otro hilo gano la carrera. El dia queda marcado igualmente, que es lo unico
            // que le importa a quien llamo.
            return false;
        }
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
