package com.constellations.habits.domain.user;

import com.constellations.habits.domain.ValidationException;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Codigo personal y permanente con el que un usuario invita a otros.
 *
 * <p>El alfabeto excluye O, 0, I y 1: el codigo se dicta en voz alta y se teclea a mano,
 * y esos cuatro caracteres son la principal fuente de errores al transcribirlo.
 *
 * <p>Son 32^8 combinaciones (~1,1 billones). Suficiente para que adivinar uno al azar no
 * sea practico, siempre que el endpoint que los consume este limitado por frecuencia.
 */
public record InviteCode(String value) {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private static final RandomGenerator DEFAULT_RANDOM = new SecureRandom();

    public InviteCode {
        value = normalize(value);
    }

    public static InviteCode generate() {
        return generate(DEFAULT_RANDOM);
    }

    /** Permite inyectar un generador determinista en los tests. */
    public static InviteCode generate(RandomGenerator random) {
        var builder = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new InviteCode(builder.toString());
    }

    /**
     * Acepta lo que el usuario pegue: minusculas, espacios o el guion decorativo del
     * formato de presentacion.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new ValidationException("El codigo de invitacion es obligatorio");
        }
        String cleaned = raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);

        if (cleaned.length() != LENGTH) {
            throw new ValidationException(
                    "El codigo de invitacion debe tener " + LENGTH + " caracteres");
        }
        for (int i = 0; i < cleaned.length(); i++) {
            if (ALPHABET.indexOf(cleaned.charAt(i)) < 0) {
                throw new ValidationException(
                        "El codigo de invitacion contiene caracteres no validos");
            }
        }
        return cleaned;
    }

    /** Formato para mostrar y dictar: ABCD-2345. */
    public String formatted() {
        return value.substring(0, 4) + "-" + value.substring(4);
    }
}
