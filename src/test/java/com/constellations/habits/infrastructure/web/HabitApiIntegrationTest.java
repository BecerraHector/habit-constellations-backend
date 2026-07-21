package com.constellations.habits.infrastructure.web;

import com.constellations.habits.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre la API real contra un Postgres de verdad levantado con Testcontainers,
 * incluyendo las migraciones de Flyway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class HabitApiIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void un_usuario_se_registra_crea_un_habito_y_lo_cumple() throws Exception {
        String token = registerAndLogin("astronoma@constelaciones.test");

        String habitId = createHabit(token, "Meditar 10 minutos");

        // Recien creado: sin estrellas.
        mvc.perform(get("/api/v1/habits/" + habitId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.currentStreak").value(0))
                .andExpect(jsonPath("$.progress.completedToday").value(false));

        mvc.perform(post("/api/v1/habits/" + habitId + "/completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.currentStreak").value(1))
                .andExpect(jsonPath("$.progress.completedToday").value(true))
                .andExpect(jsonPath("$.progress.starsInCurrentCycle").value(1));
    }

    @Test
    void marcar_dos_veces_el_mismo_dia_no_suma_dos_estrellas() throws Exception {
        String token = registerAndLogin("repetidor@constelaciones.test");
        String habitId = createHabit(token, "Beber agua");

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/habits/" + habitId + "/completions")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/api/v1/habits/" + habitId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.progress.currentStreak").value(1))
                .andExpect(jsonPath("$.progress.totalCompletions").value(1));
    }

    @Test
    void no_se_puede_rellenar_el_pasado_lejano() throws Exception {
        String token = registerAndLogin("tramposo@constelaciones.test");
        String habitId = createHabit(token, "Correr");

        mvc.perform(post("/api/v1/habits/" + habitId + "/completions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "%s"}""".formatted(LocalDate.now().minusDays(30))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void no_se_puede_marcar_una_fecha_futura() throws Exception {
        String token = registerAndLogin("adivino@constelaciones.test");
        String habitId = createHabit(token, "Leer");

        mvc.perform(post("/api/v1/habits/" + habitId + "/completions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "%s"}""".formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void el_habito_de_otro_usuario_no_existe() throws Exception {
        String ownerToken = registerAndLogin("duena@constelaciones.test");
        String habitId = createHabit(ownerToken, "Diario personal");

        String intruderToken = registerAndLogin("intruso@constelaciones.test");

        // 404 y no 403: revelar que el id existe ya seria filtrar informacion.
        mvc.perform(get("/api/v1/habits/" + habitId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void sin_token_la_api_responde_401() throws Exception {
        mvc.perform(get("/api/v1/habits")).andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_con_la_firma_manipulada_no_sirve() throws Exception {
        String token = registerAndLogin("falsificador@constelaciones.test");

        // Se altera el ultimo caracter de la firma. Anadir uno no valdria: ver
        // JwtServiceTest, donde se documenta por que un caracter suelto se descarta.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        mvc.perform(get("/api/v1/habits").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void no_se_puede_registrar_dos_veces_el_mismo_email() throws Exception {
        registerAndLogin("duplicada@constelaciones.test");

        mvc.perform(register("duplicada@constelaciones.test"))
                .andExpect(status().isConflict());
    }

    @Test
    void el_email_no_distingue_mayusculas() throws Exception {
        registerAndLogin("mayusculas@constelaciones.test");

        mvc.perform(register("MAYUSCULAS@Constelaciones.TEST"))
                .andExpect(status().isConflict());
    }

    @Test
    void una_contrasena_corta_se_rechaza() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"corta@constelaciones.test","password":"123",
                                 "displayName":"Corta","zoneId":"UTC"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void el_habito_archivado_desaparece_del_listado() throws Exception {
        String token = registerAndLogin("archivadora@constelaciones.test");
        String habitId = createHabit(token, "Habito temporal");

        mvc.perform(delete("/api/v1/habits/" + habitId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/habits").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.RequestBuilder register(String email) {
        return post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"constelacion-secreta","displayName":"Test",
                         "zoneId":"America/Lima"}""".formatted(email));
    }

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(register(email)).andExpect(status().isCreated());

        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");
        assertThat(token).isNotBlank();
        return token;
    }

    private String createHabit(String token, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/habits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":null}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(body, "$.id");
    }
}
