package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.domain.social.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataFriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {

    /** Busca la relacion sin importar quien inicio la solicitud. */
    @Query("""
            SELECT f FROM FriendshipEntity f
            WHERE (f.requesterId = :a AND f.addresseeId = :b)
               OR (f.requesterId = :b AND f.addresseeId = :a)
            """)
    Optional<FriendshipEntity> findBetween(@Param("a") UUID userA, @Param("b") UUID userB);

    @Query("""
            SELECT f FROM FriendshipEntity f
            WHERE f.status = :status
              AND (f.requesterId = :userId OR f.addresseeId = :userId)
            ORDER BY f.respondedAt DESC
            """)
    Page<FriendshipEntity> findByStatusInvolving(
            @Param("userId") UUID userId,
            @Param("status") FriendshipStatus status,
            Pageable pageable);

    List<FriendshipEntity> findByAddresseeIdAndStatusOrderByCreatedAtDesc(
            UUID addresseeId, FriendshipStatus status);

    List<FriendshipEntity> findByRequesterIdAndStatusOrderByCreatedAtDesc(
            UUID requesterId, FriendshipStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FriendshipEntity f WHERE f.requesterId = :userId OR f.addresseeId = :userId")
    int deleteAllInvolving(@Param("userId") UUID userId);
}
