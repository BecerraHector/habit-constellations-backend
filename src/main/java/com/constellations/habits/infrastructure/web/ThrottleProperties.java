package com.constellations.habits.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tope duro por IP para los endpoints que aceptan credenciales o codigos a ciegas.
 *
 * <p>Complementa al retardo creciente por cuenta: aquel evita que ataquen una cuenta
 * concreta; este evita que una misma IP barra muchas cuentas o muchos codigos. Los tests
 * lo suben en su perfil para poder hacer login sin cuentagotas.
 */
@ConfigurationProperties(prefix = "habits.security.throttle")
public record ThrottleProperties(Integer ipRequestsPerMinute) {

    private static final int DEFAULT_IP_REQUESTS_PER_MINUTE = 30;

    public int ipLimit() {
        return ipRequestsPerMinute != null ? ipRequestsPerMinute : DEFAULT_IP_REQUESTS_PER_MINUTE;
    }
}
