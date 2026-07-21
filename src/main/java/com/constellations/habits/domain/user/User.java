package com.constellations.habits.domain.user;

import com.constellations.habits.domain.ValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cuenta de un usuario.
 *
 * <p>La zona horaria no es un detalle de presentacion: define donde cae la medianoche
 * del usuario y por tanto que dia se le atribuye a cada cumplimiento. Vive en el dominio.
 */
public record User(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        ZoneId zoneId,
        InviteCode inviteCode,
        Instant createdAt,
        Instant deletedAt) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");

    /** Lo que ve el grupo donde esta persona dejo estrellas antes de darse de baja. */
    public static final String TOMBSTONE_NAME = "Cuenta eliminada";

    /**
     * {@code .invalid} es un dominio reservado que no puede resolverse nunca, asi que
     * este correo no llega a ninguna parte ni colisiona con el de nadie real.
     */
    private static final String TOMBSTONE_EMAIL_DOMAIN = "@eliminado.invalid";

    /**
     * No es el hash de ninguna contrasena: BCrypt jamas produce esta cadena, de modo que
     * ninguna comparacion puede darla por buena.
     */
    private static final String UNUSABLE_PASSWORD = "cuenta-eliminada-sin-contrasena";

    public User {
        ValidationException.requirePresent(id, "id");
        email = normalizeEmail(email);
        ValidationException.requireText(passwordHash, "passwordHash", 255);
        displayName = ValidationException.requireText(displayName, "displayName", 60);
        ValidationException.requirePresent(zoneId, "zoneId");
        ValidationException.requirePresent(inviteCode, "inviteCode");
        ValidationException.requirePresent(createdAt, "createdAt");
    }

    public static User register(
            String email,
            String passwordHash,
            String displayName,
            ZoneId zoneId,
            InviteCode inviteCode,
            Instant now) {
        return new User(
                UUID.randomUUID(), email, passwordHash, displayName, zoneId, inviteCode, now, null);
    }

    /** Se usa cuando el codigo se ha difundido mas de la cuenta y hay que invalidarlo. */
    public User withInviteCode(InviteCode newCode) {
        return new User(id, email, passwordHash, displayName, zoneId, newCode, createdAt, deletedAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Convierte la cuenta en una lapida: se va todo lo que identifica a la persona y
     * queda una fila anonima.
     *
     * <p>La fila no se borra porque el brillo de los dias ya vividos depende de ella. Si
     * desapareciera, un martes que fue "3 de 3" se repintaria como "2 de 3" en la
     * pantalla de los otros dos, y borrarse la cuenta reescribiria el pasado de gente que
     * no pidio nada.
     *
     * @param tombstoneCode codigo nuevo y descartado; el anterior deja de existir para
     *                      que nadie pueda seguir usandolo para contactar
     */
    public User anonymize(InviteCode tombstoneCode, Instant now) {
        return new User(
                id,
                id + TOMBSTONE_EMAIL_DOMAIN,
                UNUSABLE_PASSWORD,
                TOMBSTONE_NAME,
                zoneId,
                tombstoneCode,
                createdAt,
                now);
    }

    /** El "hoy" del usuario, que puede no coincidir con el del servidor. */
    public LocalDate today(Instant now) {
        return LocalDate.ofInstant(now, zoneId);
    }

    public static String normalizeEmail(String raw) {
        String value = ValidationException.requireText(raw, "email", 255).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(value).matches()) {
            throw new ValidationException("email no tiene un formato valido");
        }
        return value;
    }

    /**
     * Evita que el hash de la contrasena y el codigo de invitacion acaben en un log por
     * accidente: el codigo es, de hecho, una credencial para contactar al usuario.
     */
    @Override
    public String toString() {
        return "User[id=%s, email=%s, displayName=%s, zoneId=%s]"
                .formatted(id, email, displayName, zoneId);
    }
}
