package com.constellations.habits.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface SpringDataHabitLogRepository extends JpaRepository<HabitLogEntity, UUID> {

    @Query("SELECT l.logDate FROM HabitLogEntity l WHERE l.habitId = :habitId ORDER BY l.logDate")
    List<LocalDate> findLogDates(@Param("habitId") UUID habitId);

    /** Proyeccion (habitId, logDate) para resolver el panel entero en una consulta. */
    @Query("""
            SELECT l.habitId, l.logDate FROM HabitLogEntity l
            WHERE l.habitId IN :habitIds
            ORDER BY l.logDate
            """)
    List<Object[]> findLogDatesForHabits(@Param("habitIds") List<UUID> habitIds);

    /** Misma proyeccion, acotada a la ventana que el mapa de brillo va a pintar. */
    @Query("""
            SELECT l.habitId, l.logDate FROM HabitLogEntity l
            WHERE l.habitId IN :habitIds AND l.logDate BETWEEN :from AND :to
            ORDER BY l.logDate
            """)
    List<Object[]> findLogDatesForHabitsBetween(
            @Param("habitIds") List<UUID> habitIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    boolean existsByHabitIdAndLogDate(UUID habitId, LocalDate logDate);

    long deleteByHabitIdAndLogDate(UUID habitId, LocalDate logDate);
}
