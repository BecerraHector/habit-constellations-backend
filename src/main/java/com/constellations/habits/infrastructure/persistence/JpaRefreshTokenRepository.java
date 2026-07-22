package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.port.out.RefreshTokenRepository;
import com.constellations.habits.domain.user.RefreshToken;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository delegate;

    JpaRefreshTokenRepository(SpringDataRefreshTokenRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return toDomain(delegate.save(toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return delegate.findByTokenHash(tokenHash).map(JpaRefreshTokenRepository::toDomain);
    }

    @Override
    @Transactional
    public int revokeAllForUser(UUID userId, Instant now) {
        return delegate.revokeAllForUser(userId, now);
    }

    @Override
    @Transactional
    public void deleteAllForUser(UUID userId) {
        delegate.deleteAllForUser(userId);
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant now) {
        return delegate.deleteExpiredBefore(now);
    }

    private static RefreshTokenEntity toEntity(RefreshToken token) {
        var entity = new RefreshTokenEntity();
        entity.setId(token.id());
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setIssuedAt(token.issuedAt());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevokedAt(token.revokedAt());
        return entity;
    }

    private static RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt());
    }
}
