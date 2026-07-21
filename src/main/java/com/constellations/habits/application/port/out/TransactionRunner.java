package com.constellations.habits.application.port.out;

import java.util.function.Supplier;

/**
 * Delimita una unidad de trabajo atomica.
 *
 * <p>Existe como puerto y no como anotacion porque los casos de uso son POJOs sin
 * dependencias de framework: {@code @Transactional} obligaria a importar Spring en la
 * capa de aplicacion, que es justo lo que {@code LayeringTest} prohibe.
 *
 * <p>La ventaja no es solo de pureza. Al ser una llamada explicita, la frontera se ve
 * leyendo el metodo, en lugar de depender de un proxy invisible que ademas se desactiva
 * en silencio cuando un metodo se llama a si mismo.
 */
public interface TransactionRunner {

    <T> T execute(Supplier<T> work);

    default void run(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
