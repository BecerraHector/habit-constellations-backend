package com.constellations.habits.application.exception;

/**
 * El token de refresco no existe, ya caduco o fue revocado.
 *
 * <p>El mensaje no distingue entre los tres casos, por la misma razon por la que el
 * login no distingue "email inexistente" de "clave incorrecta".
 */
public class InvalidRefreshTokenException extends ApplicationException {

    public InvalidRefreshTokenException() {
        super("El token de refresco no es valido");
    }
}
