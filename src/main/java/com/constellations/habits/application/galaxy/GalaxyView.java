package com.constellations.habits.application.galaxy;

import com.constellations.habits.domain.galaxy.Galaxy;
import com.constellations.habits.domain.galaxy.GalaxyMembership;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una galaxia tal y como la ve un usuario concreto.
 *
 * @param joinedOn cuando se unio quien mira, o {@code null} si no pertenece. Es
 *                 informacion distinta de la fecha en que empezo su habito: el habito
 *                 puede llevar meses vivo y la pertenencia ser de ayer
 * @param habitId  su habito enlazado, para que el cliente pueda saltar al panel personal
 *                 en vez de repetir aqui la racha
 */
public record GalaxyView(
        UUID id,
        String name,
        String description,
        String theme,
        UUID creatorId,
        Instant createdAt,
        int activeMembers,
        boolean member,
        LocalDate joinedOn,
        UUID habitId) {

    public static GalaxyView of(Galaxy galaxy, int activeMembers, GalaxyMembership mine) {
        return new GalaxyView(
                galaxy.id(),
                galaxy.name(),
                galaxy.description(),
                galaxy.theme().value(),
                galaxy.creatorId(),
                galaxy.createdAt(),
                activeMembers,
                mine != null,
                mine != null ? mine.joinedOn() : null,
                mine != null ? mine.habitId() : null);
    }
}
