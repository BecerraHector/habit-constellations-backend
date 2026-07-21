package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.domain.social.FriendshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friendships")
@Getter
@Setter
@NoArgsConstructor
class FriendshipEntity {

    @Id
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "addressee_id", nullable = false)
    private UUID addresseeId;

    /**
     * Se guarda como texto y no como ordinal: reordenar el enum no debe reinterpretar
     * silenciosamente las filas ya escritas.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendshipStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;
}
