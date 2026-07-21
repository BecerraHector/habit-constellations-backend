package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.port.out.FriendshipRepository;
import com.constellations.habits.domain.social.Friendship;
import com.constellations.habits.domain.social.FriendshipStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaFriendshipRepository implements FriendshipRepository {

    private final SpringDataFriendshipRepository delegate;

    JpaFriendshipRepository(SpringDataFriendshipRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Friendship save(Friendship friendship) {
        return toDomain(delegate.save(toEntity(friendship)));
    }

    @Override
    public Optional<Friendship> findById(UUID id) {
        return delegate.findById(id).map(JpaFriendshipRepository::toDomain);
    }

    @Override
    public Optional<Friendship> findBetween(UUID userA, UUID userB) {
        return delegate.findBetween(userA, userB).map(JpaFriendshipRepository::toDomain);
    }

    @Override
    public List<Friendship> findAcceptedFor(UUID userId) {
        return map(delegate.findByStatusInvolving(userId, FriendshipStatus.ACCEPTED));
    }

    @Override
    public List<Friendship> findPendingReceivedBy(UUID userId) {
        return map(delegate.findByAddresseeIdAndStatusOrderByCreatedAtDesc(
                userId, FriendshipStatus.PENDING));
    }

    @Override
    public List<Friendship> findPendingSentBy(UUID userId) {
        return map(delegate.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                userId, FriendshipStatus.PENDING));
    }

    @Override
    public void delete(UUID friendshipId) {
        delegate.deleteById(friendshipId);
    }

    private static List<Friendship> map(List<FriendshipEntity> entities) {
        return entities.stream().map(JpaFriendshipRepository::toDomain).toList();
    }

    private static FriendshipEntity toEntity(Friendship friendship) {
        var entity = new FriendshipEntity();
        entity.setId(friendship.id());
        entity.setRequesterId(friendship.requesterId());
        entity.setAddresseeId(friendship.addresseeId());
        entity.setStatus(friendship.status());
        entity.setCreatedAt(friendship.createdAt());
        entity.setRespondedAt(friendship.respondedAt());
        return entity;
    }

    private static Friendship toDomain(FriendshipEntity entity) {
        return new Friendship(
                entity.getId(),
                entity.getRequesterId(),
                entity.getAddresseeId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRespondedAt());
    }
}
