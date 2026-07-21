package com.constellations.habits.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "habit_logs")
@Getter
@Setter
@NoArgsConstructor
class HabitLogEntity {

    @Id
    private UUID id;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
}
