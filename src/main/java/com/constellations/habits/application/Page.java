package com.constellations.habits.application;

import java.util.List;
import java.util.function.Function;

/**
 * Un tramo de resultados junto con lo necesario para pedir el siguiente.
 *
 * <p>Se devuelve {@code totalElements} ademas de {@code hasNext} porque son dos preguntas
 * distintas: la primera permite escribir "12 amigos" en la pantalla, y la segunda decidir
 * si se pinta el boton de cargar mas sin tener que calcularlo restando.
 */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }

    public static <T> Page<T> empty(PageQuery query) {
        return new Page<>(List.of(), query.page(), query.size(), 0);
    }

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }

    /** Cambia el contenido conservando los datos de paginacion. */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}
