package com.constellations.habits.application;

/**
 * Tramo de un listado que el cliente pide.
 *
 * <p>El tamano se acota en el constructor y no en el controlador: si la validacion
 * viviera en el borde, cualquier caso de uso nuevo tendria que acordarse de repetirla, y
 * bastaria olvidarla una vez para permitir {@code size=1000000}.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    /** Tolera los nulos que llegan de parametros de consulta ausentes. */
    public static PageQuery of(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public static PageQuery first() {
        return new PageQuery(0, DEFAULT_SIZE);
    }
}
