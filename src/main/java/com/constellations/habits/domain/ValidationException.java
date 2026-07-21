package com.constellations.habits.domain;

/** Un valor no cumple las invariantes del modelo. */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " no puede estar vacio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field + " no puede superar " + maxLength + " caracteres");
        }
        return trimmed;
    }

    public static <T> T requirePresent(T value, String field) {
        if (value == null) {
            throw new ValidationException(field + " es obligatorio");
        }
        return value;
    }
}
