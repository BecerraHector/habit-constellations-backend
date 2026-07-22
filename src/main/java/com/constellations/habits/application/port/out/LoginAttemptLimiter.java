package com.constellations.habits.application.port.out;

import java.time.Duration;

/**
 * Freno a la fuerza bruta sobre el login, por cuenta.
 *
 * <p>La clave es el email normalizado, exista o no la cuenta: si solo se frenaran los
 * emails registrados, la propia cadencia del limite revelaria cuales existen. El caso de
 * uso pregunta antes de verificar nada y anota cada fallo; el exito limpia el contador,
 * para que equivocarse un par de veces no persiga al usuario legitimo.
 */
public interface LoginAttemptLimiter {

    /** Cuanto falta para poder intentarlo de nuevo. {@link Duration#ZERO} si ya se puede. */
    Duration retryAfter(String key);

    void recordFailure(String key);

    /** Un login correcto perdona los fallos anteriores. */
    void clear(String key);
}
