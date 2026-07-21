package com.constellations.habits.domain.galaxy;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * La pertenencia de un usuario a una galaxia, y el habito personal que la alimenta.
 *
 * <p>Las filas de quienes se fueron <strong>no se borran</strong>: el brillo de un dia
 * pasado se divide entre los miembros que habia <em>ese</em> dia, asi que hace falta
 * saber cuando entro y cuando salio cada uno. Si se borraran, el mapa se reescribiria
 * entero cada vez que alguien abandona.
 *
 * @param joinedOn fecha local del usuario al unirse; el primer dia que pesa en el brillo
 * @param leftOn   fecha local al salir. Excluyente: el dia que te vas ya no cuentas,
 *                 para no ensombrecerlo con una ausencia de la que ya no formas parte
 */
public record GalaxyMembership(
        UUID id,
        UUID galaxyId,
        UUID userId,
        UUID habitId,
        LocalDate joinedOn,
        Instant joinedAt,
        LocalDate leftOn,
        Instant leftAt) {

    public GalaxyMembership {
        ValidationException.requirePresent(id, "id");
        ValidationException.requirePresent(galaxyId, "galaxyId");
        ValidationException.requirePresent(userId, "userId");
        ValidationException.requirePresent(habitId, "habitId");
        ValidationException.requirePresent(joinedOn, "joinedOn");
        ValidationException.requirePresent(joinedAt, "joinedAt");
        if (leftOn != null && leftOn.isBefore(joinedOn)) {
            throw new ValidationException("leftOn no puede ser anterior a joinedOn");
        }
    }

    public static GalaxyMembership join(
            UUID galaxyId, UUID userId, UUID habitId, LocalDate today, Instant now) {
        return new GalaxyMembership(
                UUID.randomUUID(), galaxyId, userId, habitId, today, now, null, null);
    }

    public boolean isActive() {
        return leftOn == null;
    }

    /** Si esta persona pesaba en el brillo del dia indicado. */
    public boolean isActiveOn(LocalDate day) {
        return !day.isBefore(joinedOn) && (leftOn == null || day.isBefore(leftOn));
    }

    public GalaxyMembership leave(LocalDate today, Instant now) {
        if (!isActive()) {
            throw new NotAMemberException("Ya habias abandonado esta galaxia");
        }
        return new GalaxyMembership(
                id, galaxyId, userId, habitId, joinedOn, joinedAt, today, now);
    }
}
