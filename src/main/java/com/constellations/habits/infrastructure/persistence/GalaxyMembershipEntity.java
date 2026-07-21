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
@Table(name = "galaxy_memberships")
@Getter
@Setter
@NoArgsConstructor
class GalaxyMembershipEntity {

    @Id
    private UUID id;

    @Column(name = "galaxy_id", nullable = false)
    private UUID galaxyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "joined_on", nullable = false)
    private LocalDate joinedOn;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_on")
    private LocalDate leftOn;

    @Column(name = "left_at")
    private Instant leftAt;
}
