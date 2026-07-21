package com.constellations.habits.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "galaxies")
@Getter
@Setter
@NoArgsConstructor
class GalaxyEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Slug ya normalizado por el dominio; aqui solo se almacena. */
    @Column(nullable = false, length = 32)
    private String theme;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
