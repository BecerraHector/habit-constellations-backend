package com.constellations.habits.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion navegable en {@code /swagger-ui.html}, con el esquema en
 * {@code /v3/api-docs}.
 *
 * <p>Se declara el esquema de seguridad una sola vez y se aplica a toda la API para que
 * el boton "Authorize" sirva de verdad: sin el, cada endpoint protegido responderia 401
 * desde la propia pagina y la documentacion no valdria para probar nada.
 */
@Configuration
class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI habitTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Habit Tracker — Forja de Constelaciones")
                        .version("v1")
                        .description("""
                                API de seguimiento de habitos: cada dia cumplido es una \
                                estrella y cada racha sostenida traza una constelacion. \
                                Las constelaciones compartidas anaden un mapa de brillo \
                                proporcional a cuanta gente del grupo cumplio ese dia."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
