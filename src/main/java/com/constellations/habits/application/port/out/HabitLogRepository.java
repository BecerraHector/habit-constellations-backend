package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.habit.HabitLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HabitLogRepository {

    HabitLog save(HabitLog log);

    /**
     * Todas las fechas cumplidas de un habito. El motor de rachas necesita el historial
     * completo para calcular la mejor racha y las constelaciones ya ganadas.
     */
    List<LocalDate> findDatesByHabit(UUID habitId);

    /**
     * Version por lotes para no lanzar una consulta por habito al listar el panel
     * del usuario (problema N+1).
     */
    Map<UUID, List<LocalDate>> findDatesByHabits(List<UUID> habitIds);

    boolean existsByHabitAndDate(UUID habitId, LocalDate logDate);

    /** @return true si habia un log que borrar. */
    boolean deleteByHabitAndDate(UUID habitId, LocalDate logDate);
}
