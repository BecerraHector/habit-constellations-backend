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
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre las constelaciones compartidas contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GalaxyApiIntegrationTest {

    /** Emails y temas unicos por test: el contexto de Spring se comparte entre todos. */
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private static final String ZONE = "America/Lima";

    @Autowired MockMvc mvc;

    @Test
    void crear_una_galaxia_deja_el_habito_en_el_panel_personal() throws Exception {
        var ana = registerUser("fundadora");

        String galaxyId = createGalaxy(ana.token(), "Gym a las 6", "gym", null);

        // El punto del diseno: no hay un seguimiento aparte. El habito de la galaxia
        // es un habito suyo, con su racha, en su panel de siempre.
        mvc.perform(get("/api/v1/habits").header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Gym a las 6"));

        mvc.perform(get("/api/v1/galaxies").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(galaxyId))
                .andExpect(jsonPath("$[0].member").value(true))
                .andExpect(jsonPath("$[0].activeMembers").value(1))
                .andExpect(jsonPath("$[0].habitId").isNotEmpty());
    }

    @Test
    void unirse_enlazando_un_habito_propio_no_lo_duplica() throws Exception {
        var ana = registerUser("veterana");
        var bruno = registerUser("anfitrion");

        String galaxyId = createGalaxy(bruno.token(), "Correr", "correr", null);

        // Ana ya llevaba meses corriendo por su cuenta: se enlaza ese habito y no se
        // le crea uno nuevo, o tendria dos rachas del mismo esfuerzo.
        String suyo = createHabit(ana.token(), "Correr por el malecon");
        join(ana.token(), galaxyId, suyo);

        mvc.perform(get("/api/v1/habits").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(suyo));

        mvc.perform(get("/api/v1/galaxies/" + galaxyId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.galaxy.habitId").value(suyo))
                .andExpect(jsonPath("$.galaxy.activeMembers").value(2));
    }

    @Test
    void el_brillo_de_hoy_sube_conforme_cumple_mas_gente() throws Exception {
        var ana = registerUser("brillo-a");
        var bruno = registerUser("brillo-b");

        String galaxyId = createGalaxy(ana.token(), "Estudiar", "estudio", null);
        String suyoDeAna = habitOf(ana.token(), galaxyId);
        String suyoDeBruno = joinAndGetHabit(bruno.token(), galaxyId);

        // Nadie ha cumplido: la estrella existe, apagada. No desaparece.
        mvc.perform(get("/api/v1/galaxies/" + galaxyId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.map.days[0].level").value(0))
                .andExpect(jsonPath("$.map.days[0].activeMembers").value(2))
                .andExpect(jsonPath("$.map.maxLevel").value(4));

        complete(ana.token(), suyoDeAna);
        mvc.perform(get("/api/v1/galaxies/" + galaxyId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.map.days[0].level").value(2))
                .andExpect(jsonPath("$.map.days[0].completions").value(1))
                .andExpect(jsonPath("$.map.perfectDays").value(0));

        complete(bruno.token(), suyoDeBruno);
        mvc.perform(get("/api/v1/galaxies/" + galaxyId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.map.days[0].level").value(4))
                .andExpect(jsonPath("$.map.days[0].completions").value(2))
                .andExpect(jsonPath("$.map.perfectDays").value(1))
                .andExpect(jsonPath("$.map.totalStars").value(2));
    }

    @Test
    void el_desglose_de_un_dia_nombra_a_quienes_cumplieron() throws Exception {
        var ana = registerUser("cumplidora");
        var bruno = registerUser("ausente");

        String galaxyId = createGalaxy(ana.token(), "Leer", "lectura", null);
        complete(ana.token(), habitOf(ana.token(), galaxyId));
        joinAndGetHabit(bruno.token(), galaxyId);

        String body = mvc.perform(get("/api/v1/galaxies/" + galaxyId + "/days/" + today())
                        .header("Authorization", "Bearer " + bruno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completions").value(1))
                .andExpect(jsonPath("$.activeMembers").value(2))
                .andExpect(jsonPath("$.completedBy.length()").value(1))
                .andExpect(jsonPath("$.completedBy[0]").value(ana.displayName()))
                .andReturn().getResponse().getContentAsString();

        // Se nombra a quien cumplio, nunca se lista a quien falto.
        assertThat(body).doesNotContain(bruno.displayName());
    }

    @Test
    void una_galaxia_no_revela_el_email_ni_el_codigo_ni_los_demas_habitos() throws Exception {
        var ana = registerUser("observadora");
        var bruno = registerUser("observado");

        String galaxyId = createGalaxy(bruno.token(), "Dormir 8 horas", "dormir-7-8-horas", null);
        createHabit(bruno.token(), "Terapia los martes");
        joinAndGetHabit(ana.token(), galaxyId);

        String body = mvc.perform(get("/api/v1/galaxies/" + galaxyId + "/members")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn().getResponse().getContentAsString();

        // Unirse a un grupo abierto expone el nombre visible y el habito compartido.
        // Nada mas: el email y el codigo siguen siendo credenciales, y el resto de
        // habitos de alguien no son asunto del grupo.
        assertThat(body).contains(bruno.displayName());
        assertThat(body).doesNotContain(bruno.email());
        assertThat(body).doesNotContain("Terapia");
        assertThat(body).doesNotContain("currentStreak", "longestStreak");
    }

    @Test
    void salir_conserva_el_habito_personal() throws Exception {
        var ana = registerUser("desertora");

        String galaxyId = createGalaxy(ana.token(), "Meditar", "meditacion", null);
        String habitId = habitOf(ana.token(), galaxyId);
        complete(ana.token(), habitId);

        mvc.perform(delete("/api/v1/galaxies/" + galaxyId + "/members/me")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isNoContent());

        // El habito lo empezo ella y las estrellas ganadas son suyas.
        mvc.perform(get("/api/v1/habits/" + habitId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.currentStreak").value(1));

        mvc.perform(get("/api/v1/galaxies").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void archivar_el_habito_saca_al_usuario_de_la_galaxia() throws Exception {
        var ana = registerUser("archivadora");

        String galaxyId = createGalaxy(ana.token(), "Yoga", "yoga", null);
        String habitId = habitOf(ana.token(), galaxyId);

        mvc.perform(delete("/api/v1/habits/" + habitId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isNoContent());

        // Si siguiera dentro, pesaria en el denominador sin poder marcar nada y
        // oscureceria al grupo para siempre.
        mvc.perform(get("/api/v1/galaxies").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/v1/galaxies/" + galaxyId)
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.galaxy.activeMembers").value(0))
                .andExpect(jsonPath("$.galaxy.member").value(false));
    }

    @Test
    void no_se_puede_entrar_dos_veces_en_la_misma_galaxia() throws Exception {
        var ana = registerUser("insistente");
        var bruno = registerUser("anfitriona");

        String galaxyId = createGalaxy(bruno.token(), "Agua", "beber-agua", null);
        join(ana.token(), galaxyId, null);

        mvc.perform(post("/api/v1/galaxies/" + galaxyId + "/members")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isConflict());
    }

    @Test
    void salir_de_una_galaxia_ajena_responde_404() throws Exception {
        var ana = registerUser("extrana");
        var bruno = registerUser("titular");

        String galaxyId = createGalaxy(bruno.token(), "Piano", "piano", null);

        mvc.perform(delete("/api/v1/galaxies/" + galaxyId + "/members/me")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void una_galaxia_inexistente_responde_404() throws Exception {
        var ana = registerUser("perdida");

        mvc.perform(get("/api/v1/galaxies/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void el_tema_se_normaliza_al_crear_y_al_buscar() throws Exception {
        var ana = registerUser("tematica");
        String theme = "Escalada " + SEQUENCE.incrementAndGet();
        String expected = theme.toLowerCase().replace(' ', '-');

        createGalaxy(ana.token(), "Boulder", theme, null);

        // Se busca escribiendolo de otra forma y aun asi aparece.
        mvc.perform(get("/api/v1/galaxies/discover")
                        .param("theme", "  " + theme.toUpperCase() + "  ")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].theme").value(expected));
    }

    @Test
    void el_catalogo_sugiere_temas_aunque_nadie_los_haya_estrenado() throws Exception {
        var ana = registerUser("curiosa");

        String body = mvc.perform(get("/api/v1/galaxies/catalog")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // La ruta literal no la captura {galaxyId}, y las semillas evitan un catalogo
        // vacio el primer dia.
        assertThat(body).contains("gym", "estudio", "lectura");
    }

    @Test
    void enlazar_un_habito_ajeno_responde_404() throws Exception {
        var ana = registerUser("intrusa");
        var bruno = registerUser("dueno");

        String ajeno = createHabit(bruno.token(), "Sus flexiones");
        String galaxyId = createGalaxy(bruno.token(), "Flexiones", "flexiones", null);

        mvc.perform(post("/api/v1/galaxies/" + galaxyId + "/members")
                        .header("Authorization", "Bearer " + ana.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"habitId":"%s"}""".formatted(ajeno)))
                .andExpect(status().isNotFound());
    }

    @Test
    void la_lista_de_miembros_se_pagina() throws Exception {
        var ana = registerUser("pagina-a");
        var bruno = registerUser("pagina-b");
        var carla = registerUser("pagina-c");

        String galaxyId = createGalaxy(ana.token(), "Multitud", "multitud", null);
        joinAndGetHabit(bruno.token(), galaxyId);
        joinAndGetHabit(carla.token(), galaxyId);

        mvc.perform(get("/api/v1/galaxies/" + galaxyId + "/members")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mvc.perform(get("/api/v1/galaxies/" + galaxyId + "/members")
                        .param("page", "1").param("size", "2")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void un_tamano_de_pagina_desmedido_se_recorta() throws Exception {
        var ana = registerUser("desmedida");
        String galaxyId = createGalaxy(ana.token(), "Tope", "tope", null);

        // El limite se aplica en el propio PageQuery, no en el controlador: asi no hay
        // forma de pedir un volcado entero desde ningun endpoint.
        mvc.perform(get("/api/v1/galaxies/" + galaxyId + "/members")
                        .param("size", "100000")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void sin_token_los_endpoints_de_galaxias_responden_401() throws Exception {
        mvc.perform(get("/api/v1/galaxies")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/galaxies/catalog")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/galaxies/discover")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private record TestUser(String id, String email, String displayName, String token) {}

    private static String today() {
        return LocalDate.now(ZoneId.of(ZONE)).toString();
    }

    private TestUser registerUser(String prefix) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        String email = prefix + "-" + n + "@constelaciones.test";
        String displayName = prefix + "-" + n;

        String registerBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta",
                                 "displayName":"%s","zoneId":"%s"}"""
                                .formatted(email, displayName, ZONE)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String loginBody = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new TestUser(
                JsonPath.read(registerBody, "$.id"),
                email,
                displayName,
                JsonPath.read(loginBody, "$.accessToken"));
    }

    private String createGalaxy(String token, String name, String theme, String habitId)
            throws Exception {

        String body = mvc.perform(post("/api/v1/galaxies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":null,"theme":"%s","habitId":%s}"""
                                .formatted(name, theme, habitId == null ? "null" : "\"" + habitId + "\"")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    private String join(String token, String galaxyId, String habitId) throws Exception {
        var request = post("/api/v1/galaxies/" + galaxyId + "/members")
                .header("Authorization", "Bearer " + token);
        if (habitId != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"habitId":"%s"}""".formatted(habitId));
        }

        String body = mvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.habitId");
    }

    private String joinAndGetHabit(String token, String galaxyId) throws Exception {
        return join(token, galaxyId, null);
    }

    /** El habito que enlaza a quien pregunta con la galaxia. */
    private String habitOf(String token, String galaxyId) throws Exception {
        String body = mvc.perform(get("/api/v1/galaxies/" + galaxyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.galaxy.habitId");
    }

    private String createHabit(String token, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/habits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":null}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }

    private void complete(String token, String habitId) throws Exception {
        mvc.perform(post("/api/v1/habits/" + habitId + "/completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
