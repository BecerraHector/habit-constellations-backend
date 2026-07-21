package com.constellations.habits.infrastructure.web;

import com.constellations.habits.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sesiones de larga vida, CORS y documentacion: lo que un frontend necesita. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** El unico origen permitido en el perfil de test. */
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired MockMvc mvc;

    @Test
    void el_login_entrega_ademas_un_token_de_refresco() throws Exception {
        var session = login(register("sesion"));

        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.refreshToken()).isNotBlank();
        // Es un valor opaco, no un JWT: no tiene las dos partes separadas por punto.
        assertThat(session.refreshToken()).doesNotContain(".");
    }

    @Test
    void el_refresco_devuelve_credenciales_nuevas_y_rota_el_token() throws Exception {
        var session = login(register("rotacion"));

        var renovada = refresh(session.refreshToken());

        assertThat(renovada.refreshToken()).isNotEqualTo(session.refreshToken());
        // El acceso nuevo sirve de verdad.
        mvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + renovada.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void un_token_de_refresco_ya_usado_deja_de_valer() throws Exception {
        var session = login(register("usado"));
        refresh(session.refreshToken());

        mvc.perform(refreshRequest(session.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reutilizar_un_token_revocado_cierra_todas_las_sesiones() throws Exception {
        var session = login(register("robada"));
        var renovada = refresh(session.refreshToken());

        // Alguien presenta el token viejo: hay una copia por ahi, y no se puede saber
        // quien es el legitimo. Se cierran las dos sesiones.
        mvc.perform(refreshRequest(session.refreshToken()))
                .andExpect(status().isUnauthorized());

        mvc.perform(refreshRequest(renovada.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cerrar_sesion_invalida_el_token_de_refresco() throws Exception {
        var session = login(register("salida"));

        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(session.refreshToken())))
                .andExpect(status().isNoContent());

        mvc.perform(refreshRequest(session.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cerrar_sesion_dos_veces_no_es_un_error() throws Exception {
        var session = login(register("repetida"));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"%s"}""".formatted(session.refreshToken())))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void un_token_de_refresco_no_sirve_para_autenticar_peticiones() throws Exception {
        var session = login(register("confundido"));

        // Al ser opaco y no un JWT, el filtro lo descarta sin ninguna regla especial.
        mvc.perform(get("/api/v1/habits")
                        .header("Authorization", "Bearer " + session.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_de_refresco_inventado_se_rechaza_sin_distinguir_el_motivo() throws Exception {
        mvc.perform(refreshRequest("esto-no-es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void el_preflight_del_origen_permitido_pasa() throws Exception {
        mvc.perform(options("/api/v1/habits")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void el_preflight_de_un_origen_desconocido_se_rechaza() throws Exception {
        // Sin comodin: una pagina cualquiera de internet no es un cliente valido.
        mvc.perform(options("/api/v1/habits")
                        .header(HttpHeaders.ORIGIN, "https://sitio-cualquiera.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void la_documentacion_esta_publicada_y_describe_los_endpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/galaxies/{galaxyId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme")
                        .value("bearer"));
    }

    // --- helpers ---

    private record Session(String accessToken, String refreshToken) {}

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + SEQUENCE.incrementAndGet() + "@constelaciones.test";

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta",
                                 "displayName":"%s","zoneId":"America/Lima"}"""
                                .formatted(email, prefix)))
                .andExpect(status().isCreated());

        return email;
    }

    private Session login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new Session(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    private org.springframework.test.web.servlet.RequestBuilder refreshRequest(String token) {
        return post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}""".formatted(token));
    }

    private Session refresh(String refreshToken) throws Exception {
        String body = mvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new Session(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }
}
