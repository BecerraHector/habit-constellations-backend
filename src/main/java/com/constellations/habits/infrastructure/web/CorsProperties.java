package com.constellations.habits.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origenes desde los que un navegador puede llamar a la API.
 *
 * <p>Se enumeran a proposito en vez de permitir {@code *}: un comodin convierte
 * cualquier pagina de internet en un cliente valido, y aunque el token viaja en la
 * cabecera y no en una cookie, no hay razon para regalar esa superficie.
 *
 * @param allowedOrigins vacio (el valor por defecto en produccion) desactiva CORS por
 *                       completo, que es lo correcto si no hay frontend desplegado
 */
@ConfigurationProperties(prefix = "habits.web.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public boolean isEnabled() {
        return !allowedOrigins.isEmpty();
    }
}
