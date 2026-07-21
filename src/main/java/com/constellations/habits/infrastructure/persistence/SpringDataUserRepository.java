package com.constellations.habits.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    // LOWER(email) para casar con el indice unico ux_users_email_lower de la migracion V1.
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = :email")
    Optional<UserEntity> findByEmailIgnoringCase(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE LOWER(u.email) = :email")
    boolean existsByEmailIgnoringCase(@Param("email") String email);
}
