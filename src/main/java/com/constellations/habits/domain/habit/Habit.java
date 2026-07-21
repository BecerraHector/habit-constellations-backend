package com.constellations.habits.domain.habit;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.util.UUID;

/**
 * Un habito diario: se cumple o no se cumple cada dia.
 *
 * <p>Se archiva en lugar de borrarse, porque sus logs son el historial del usuario
 * y las constelaciones ya ganadas no deberian desaparecer.
 */
public record Habit(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        Instant createdAt,
        Instant archivedAt) {

    public Habit {
        ValidationException.requirePresent(id, "id");
        ValidationException.requirePresent(ownerId, "ownerId");
        name = ValidationException.requireText(name, "name", 80);
        if (description != null) {
            description = description.isBlank() ? null : ValidationException.requireText(description, "description", 280);
        }
        ValidationException.requirePresent(createdAt, "createdAt");
    }

    public static Habit create(UUID ownerId, String name, String description, Instant now) {
        return new Habit(UUID.randomUUID(), ownerId, name, description, now, null);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    public Habit rename(String newName, String newDescription) {
        requireActive();
        return new Habit(id, ownerId, newName, newDescription, createdAt, archivedAt);
    }

    public Habit archive(Instant now) {
        requireActive();
        return new Habit(id, ownerId, name, description, createdAt, now);
    }

    /**
     * Deja el habito sin nombre y archivado, para cuando su dueno borra la cuenta pero
     * sus registros alimentan el mapa de una galaxia.
     *
     * <p>El nombre es lo unico personal que hay aqui —"Terapia los martes" dice mucho de
     * alguien—; una fila {@code (habito, fecha)} sin el es un recuento anonimo. A
     * diferencia de {@link #rename}, funciona tambien sobre un habito ya archivado:
     * borrarse la cuenta no puede fallar porque algo estuviera archivado de antes.
     */
    public Habit anonymize(String placeholder, Instant now) {
        return new Habit(
                id, ownerId, placeholder, null, createdAt, archivedAt != null ? archivedAt : now);
    }

    private void requireActive() {
        if (isArchived()) {
            throw new ArchivedHabitException(id);
        }
    }
}
