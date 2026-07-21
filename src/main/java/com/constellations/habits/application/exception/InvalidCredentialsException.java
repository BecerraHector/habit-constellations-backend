package com.constellations.habits.application.exception;

public class InvalidCredentialsException extends ApplicationException {

    /**
     * Mensaje deliberadamente vago: distinguir "no existe" de "clave incorrecta"
     * permitiria enumerar que emails estan registrados.
     */
    public InvalidCredentialsException() {
        super("Email o contrasena incorrectos");
    }
}
