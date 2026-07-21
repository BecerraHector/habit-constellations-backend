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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre el flujo social completo contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SocialApiIntegrationTest {

    /** Emails unicos por test: el contexto de Spring se comparte entre todos. */
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired MockMvc mvc;

    @Test
    void dos_usuarios_se_hacen_amigos_a_traves_del_codigo() throws Exception {
        var ana = registerUser("ana");
        var bruno = registerUser("bruno");

        String requestId = sendRequest(ana.token(), bruno.inviteCode());

        // Bruno la ve entrante; Ana la ve saliente.
        mvc.perform(get("/api/v1/friend-requests").header("Authorization", "Bearer " + bruno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].direction").value("INCOMING"))
                .andExpect(jsonPath("$[0].userId").value(ana.id()));

        mvc.perform(get("/api/v1/friend-requests/sent").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$[0].direction").value("OUTGOING"));

        // Antes de aceptar, nadie es amigo de nadie.
        mvc.perform(get("/api/v1/friends").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.length()").value(0));

        accept(bruno.token(), requestId);

        mvc.perform(get("/api/v1/friends").header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(bruno.id()));

        // La amistad es simetrica: Bruno tambien ve a Ana.
        mvc.perform(get("/api/v1/friends").header("Authorization", "Bearer " + bruno.token()))
                .andExpect(jsonPath("$[0].userId").value(ana.id()));
    }

    @Test
    void el_solicitante_no_puede_aceptar_su_propia_solicitud() throws Exception {
        var ana = registerUser("solicitante");
        var bruno = registerUser("destinatario");

        String requestId = sendRequest(ana.token(), bruno.inviteCode());

        mvc.perform(post("/api/v1/friend-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isConflict());
    }

    @Test
    void un_tercero_no_ve_ni_puede_responder_una_solicitud_ajena() throws Exception {
        var ana = registerUser("emisora");
        var bruno = registerUser("receptor");
        var carla = registerUser("intrusa");

        String requestId = sendRequest(ana.token(), bruno.inviteCode());

        mvc.perform(post("/api/v1/friend-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + carla.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void el_resumen_de_un_amigo_no_revela_los_nombres_de_sus_habitos() throws Exception {
        var ana = registerUser("observadora");
        var bruno = registerUser("observado");

        createAndCompleteHabit(bruno.token(), "Terapia los martes");
        accept(bruno.token(), sendRequest(ana.token(), bruno.inviteCode()));

        String body = mvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeHabits").value(1))
                .andExpect(jsonPath("$[0].bestCurrentStreak").value(1))
                .andExpect(jsonPath("$[0].totalStars").value(1))
                .andExpect(jsonPath("$[0].completedToday").value(1))
                .andReturn().getResponse().getContentAsString();

        // Lo importante: el nombre del habito no aparece por ningun lado.
        assertThat(body).doesNotContain("Terapia");
    }

    @Test
    void el_resumen_de_un_amigo_no_expone_su_email_ni_su_codigo() throws Exception {
        var ana = registerUser("curiosa");
        var bruno = registerUser("privado");

        accept(bruno.token(), sendRequest(ana.token(), bruno.inviteCode()));

        String body = mvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + ana.token()))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(bruno.email()).doesNotContain(bruno.inviteCode());
    }

    @Test
    void no_se_puede_usar_el_propio_codigo() throws Exception {
        var ana = registerUser("solitaria");

        mvc.perform(sendRequestBuilder(ana.token(), ana.inviteCode()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void no_se_puede_solicitar_dos_veces_a_la_misma_persona() throws Exception {
        var ana = registerUser("insistente");
        var bruno = registerUser("insistido");

        sendRequest(ana.token(), bruno.inviteCode());

        mvc.perform(sendRequestBuilder(ana.token(), bruno.inviteCode()))
                .andExpect(status().isConflict());
    }

    @Test
    void una_solicitud_cruzada_tampoco_crea_una_segunda_relacion() throws Exception {
        var ana = registerUser("cruzada-a");
        var bruno = registerUser("cruzada-b");

        sendRequest(ana.token(), bruno.inviteCode());

        // Bruno intenta invitar a Ana sin haber respondido a la suya.
        mvc.perform(sendRequestBuilder(bruno.token(), ana.inviteCode()))
                .andExpect(status().isConflict());
    }

    @Test
    void un_codigo_inexistente_responde_404() throws Exception {
        var ana = registerUser("perdida");

        mvc.perform(sendRequestBuilder(ana.token(), "ZZZZ9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void un_codigo_con_formato_invalido_responde_400() throws Exception {
        var ana = registerUser("despistada");

        // Contiene O y 0, que el alfabeto excluye.
        mvc.perform(sendRequestBuilder(ana.token(), "ABC0OOO1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void el_codigo_admite_guion_y_minusculas() throws Exception {
        var ana = registerUser("formateadora");
        var bruno = registerUser("formateado");

        String pretty = bruno.inviteCode().substring(0, 4) + "-" + bruno.inviteCode().substring(4);

        mvc.perform(sendRequestBuilder(ana.token(), pretty.toLowerCase()))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazar_una_solicitud_impide_reenviarla() throws Exception {
        var ana = registerUser("rechazada");
        var bruno = registerUser("rechazador");

        String requestId = sendRequest(ana.token(), bruno.inviteCode());

        mvc.perform(post("/api/v1/friend-requests/" + requestId + "/decline")
                        .header("Authorization", "Bearer " + bruno.token()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/friends").header("Authorization", "Bearer " + ana.token()))
                .andExpect(jsonPath("$.length()").value(0));

        // La relacion rechazada se conserva: no se puede insistir en bucle.
        mvc.perform(sendRequestBuilder(ana.token(), bruno.inviteCode()))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminar_a_un_amigo_permite_volver_a_invitarle() throws Exception {
        var ana = registerUser("reconciliada");
        var bruno = registerUser("reconciliado");

        accept(bruno.token(), sendRequest(ana.token(), bruno.inviteCode()));

        mvc.perform(delete("/api/v1/friends/" + bruno.id())
                        .header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/friends").header("Authorization", "Bearer " + bruno.token()))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(sendRequestBuilder(ana.token(), bruno.inviteCode()))
                .andExpect(status().isCreated());
    }

    @Test
    void regenerar_el_codigo_invalida_el_anterior() throws Exception {
        var bruno = registerUser("regenerador");
        var ana = registerUser("con-codigo-viejo");

        String newCode = JsonPath.read(
                mvc.perform(post("/api/v1/me/invite-code")
                                .header("Authorization", "Bearer " + bruno.token()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.inviteCode");

        assertThat(newCode.replace("-", "")).isNotEqualTo(bruno.inviteCode());

        mvc.perform(sendRequestBuilder(ana.token(), bruno.inviteCode()))
                .andExpect(status().isNotFound());

        mvc.perform(sendRequestBuilder(ana.token(), newCode))
                .andExpect(status().isCreated());
    }

    @Test
    void sin_token_los_endpoints_sociales_responden_401() throws Exception {
        mvc.perform(get("/api/v1/friends")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/friend-requests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me/invite-code")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private record TestUser(String id, String email, String token, String inviteCode) {}

    private TestUser registerUser(String prefix) throws Exception {
        String email = prefix + "-" + SEQUENCE.incrementAndGet() + "@constelaciones.test";

        String registerBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta",
                                 "displayName":"%s","zoneId":"America/Lima"}"""
                                .formatted(email, prefix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String loginBody = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"constelacion-secreta"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(registerBody, "$.id");
        String code = ((String) JsonPath.read(registerBody, "$.inviteCode")).replace("-", "");
        String token = JsonPath.read(loginBody, "$.accessToken");

        assertThat(UUID.fromString(id)).isNotNull();
        return new TestUser(id, email, token, code);
    }

    private org.springframework.test.web.servlet.RequestBuilder sendRequestBuilder(
            String token, String inviteCode) {
        return post("/api/v1/friend-requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"inviteCode":"%s"}""".formatted(inviteCode));
    }

    private String sendRequest(String token, String inviteCode) throws Exception {
        String body = mvc.perform(sendRequestBuilder(token, inviteCode))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.requestId");
    }

    private void accept(String addresseeToken, String requestId) throws Exception {
        mvc.perform(post("/api/v1/friend-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + addresseeToken))
                .andExpect(status().isNoContent());
    }

    private void createAndCompleteHabit(String token, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/habits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":null}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String habitId = JsonPath.read(body, "$.id");
        mvc.perform(post("/api/v1/habits/" + habitId + "/completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
