package com.constellations.habits.infrastructure.security;

import com.constellations.habits.application.port.out.LoginAttemptLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retardo creciente por cuenta, en memoria.
 *
 * <p>Los primeros fallos son gratis (equivocarse es humano); a partir de ahi cada fallo
 * duplica la espera hasta un tope. El estado vive en el proceso: si algun dia hay varias
 * instancias detras de un balanceador, esto debe mudarse a un almacen compartido, y esa
 * carencia esta anotada en PENDIENTES.md.
 */
public class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

    /** Fallos sin castigo: los dedos torpes no son un ataque. */
    static final int FREE_FAILURES = 3;

    static final Duration BASE_DELAY = Duration.ofSeconds(30);
    static final Duration MAX_DELAY = Duration.ofMinutes(15);

    /** Una cuenta sin fallos nuevos durante este tiempo se olvida por completo. */
    static final Duration FORGET_AFTER = Duration.ofHours(1);

    /** Por encima de esto, cada escritura poda entradas viejas para acotar la memoria. */
    private static final int PRUNE_THRESHOLD = 50_000;

    private record Attempts(int failures, Instant lastFailure) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryLoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Duration retryAfter(String key) {
        Attempts current = attempts.get(key);
        if (current == null || current.failures() <= FREE_FAILURES) {
            return Duration.ZERO;
        }

        Instant now = clock.instant();
        if (Duration.between(current.lastFailure(), now).compareTo(FORGET_AFTER) >= 0) {
            attempts.remove(key, current);
            return Duration.ZERO;
        }

        Instant blockedUntil = current.lastFailure().plus(delayFor(current.failures()));
        Duration remaining = Duration.between(now, blockedUntil);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    @Override
    public void recordFailure(String key) {
        Instant now = clock.instant();
        attempts.merge(key, new Attempts(1, now), (old, fresh) ->
                new Attempts(old.failures() + 1, now));
        pruneIfCrowded(now);
    }

    @Override
    public void clear(String key) {
        attempts.remove(key);
    }

    /** 4o fallo: 30s; 5o: 1m; 6o: 2m... hasta el tope. */
    private static Duration delayFor(int failures) {
        int steps = failures - FREE_FAILURES - 1;
        Duration delay = BASE_DELAY;
        for (int i = 0; i < steps && delay.compareTo(MAX_DELAY) < 0; i++) {
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }

    /**
     * Poda perezosa: sin hilos propios ni temporizadores. Solo se dispara si el mapa
     * crece de forma anomala, que es exactamente el escenario de un ataque disperso.
     */
    private void pruneIfCrowded(Instant now) {
        if (attempts.size() <= PRUNE_THRESHOLD) {
            return;
        }
        attempts.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().lastFailure(), now).compareTo(FORGET_AFTER) >= 0);
    }
}
