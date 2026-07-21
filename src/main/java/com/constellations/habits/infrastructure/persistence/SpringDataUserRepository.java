package com.constellations.habits.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    // LOWER(email) para casar con el indice unico ux_users_email_lower de la migracion V1.
    // Las lapidas quedan fuera: una cuenta dada de baja no debe poder autenticarse ni
    // aunque su correo sintetico llegara a coincidir con algo.
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = :email AND u.deletedAt IS NULL")
    Optional<UserEntity> findByEmailIgnoringCase(@Param("email") String email);

    // Este si mira todas las filas: el indice unico tampoco distingue, y decir "libre"
    // sobre un email que el indice va a rechazar solo produce un error mas confuso.
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE LOWER(u.email) = :email")
    boolean existsByEmailIgnoringCase(@Param("email") String email);

    /** Sin lapidas: nadie debe poder enviar una solicitud a una cuenta que ya no existe. */
    Optional<UserEntity> findByInviteCodeAndDeletedAtIsNull(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
