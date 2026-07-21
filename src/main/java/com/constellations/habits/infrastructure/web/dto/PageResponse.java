package com.constellations.habits.infrastructure.web.dto;

import com.constellations.habits.application.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltorio de los listados paginados.
 *
 * <p>Se define aqui en vez de serializar el {@code Page} de Spring Data porque la forma
 * de aquel no es estable entre versiones y arrastra campos internos que el cliente no
 * deberia ver ni depender de ellos.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <S, T> PageResponse<T> of(Page<S> page, Function<? super S, T> mapper) {
        return new PageResponse<>(
                page.content().stream().<T>map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext());
    }
}
