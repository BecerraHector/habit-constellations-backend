package com.constellations.habits.domain.galaxy;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.util.UUID;

/**
 * Una constelacion compartida: un habito que varios usuarios sostienen a la vez.
 *
 * <p>No guarda ni el habito ni los miembros. El habito de cada miembro es <em>suyo</em>
 * y vive en su panel personal; la galaxia solo lo enlaza a traves de
 * {@link GalaxyMembership}. Asi un mismo cumplimiento cuenta una sola vez y no hay dos
 * rachas paralelas del mismo habito que puedan discrepar.
 */
public record Galaxy(
        UUID id,
        String name,
        String description,
        GalaxyTheme theme,
        UUID creatorId,
        Instant createdAt) {

    public Galaxy {
        ValidationException.requirePresent(id, "id");
        name = ValidationException.requireText(name, "name", 80);
        if (description != null) {
            description = description.isBlank()
                    ? null
                    : ValidationException.requireText(description, "description", 280);
        }
        ValidationException.requirePresent(theme, "theme");
        ValidationException.requirePresent(creatorId, "creatorId");
        ValidationException.requirePresent(createdAt, "createdAt");
    }

    public static Galaxy create(
            UUID creatorId, String name, String description, String rawTheme, Instant now) {
        return new Galaxy(
                UUID.randomUUID(), name, description, new GalaxyTheme(rawTheme), creatorId, now);
    }
}
