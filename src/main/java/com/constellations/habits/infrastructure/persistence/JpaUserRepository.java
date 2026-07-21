package com.constellations.habits.infrastructure.persistence;

import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.user.User;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: traduce entre el modelo de dominio y las entidades JPA. */
@Repository
class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository delegate;

    JpaUserRepository(SpringDataUserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public User save(User user) {
        return toDomain(delegate.save(toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return delegate.findById(id).map(JpaUserRepository::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return delegate.findByEmailIgnoringCase(email).map(JpaUserRepository::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return delegate.existsByEmailIgnoringCase(email);
    }

    private static UserEntity toEntity(User user) {
        var entity = new UserEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setDisplayName(user.displayName());
        entity.setZoneId(user.zoneId().getId());
        entity.setCreatedAt(user.createdAt());
        return entity;
    }

    private static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                ZoneId.of(entity.getZoneId()),
                entity.getCreatedAt());
    }
}
