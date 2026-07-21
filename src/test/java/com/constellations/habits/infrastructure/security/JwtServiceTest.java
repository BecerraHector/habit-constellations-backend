package com.constellations.habits.infrastructure.security;

import com.constellations.habits.domain.user.InviteCode;
import com.constellations.habits.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T10:00:00Z");
    private static final String SECRET = "una-clave-de-pruebas-suficientemente-larga-1234567890";

    private final User user = new User(
            UUID.randomUUID(), "orion@constelaciones.test", "hash", "Orion",
            ZoneId.of("America/Lima"), new InviteCode("RSTN2345"), NOW, null);

    private JwtService serviceAt(Instant instant) {
        var properties = new JwtProperties(
                SECRET, Duration.ofMinutes(30), Duration.ofDays(30), "habit-tracker");
        return new JwtService(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void un_token_recien_emitido_resuelve_a_su_dueno() {
        var service = serviceAt(NOW);

        var token = service.issue(user).accessToken();

        assertThat(service.resolveUserId(token)).contains(user.id());
    }

    @Test
    void un_token_con_la_firma_alterada_se_rechaza() {
        var service = serviceAt(NOW);
        String token = service.issue(user).accessToken();

        // Se cambia el ultimo caracter de la firma en vez de anadir uno: alargarla
        // podria simplemente romper el decodificado, y lo que interesa comprobar es
        // que una firma bien formada pero incorrecta tampoco pasa.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThat(service.resolveUserId(tampered)).isEmpty();
    }

    @Test
    void un_token_con_el_payload_alterado_se_rechaza() {
        var service = serviceAt(NOW);
        String[] parts = service.issue(user).accessToken().split("\\.");

        String tamperedPayload =
                parts[1].substring(0, parts[1].length() - 1) + (parts[1].endsWith("A") ? "B" : "A");

        assertThat(service.resolveUserId(parts[0] + "." + tamperedPayload + "." + parts[2]))
                .isEmpty();
    }

    /**
     * Documenta una laxitud conocida de la decodificacion base64url: la firma ocupa un
     * numero exacto de caracteres, asi que UN caracter suelto al final cae en un grupo
     * base64 incompleto y se descarta al decodificar. Con dos o mas ya falla.
     *
     * <p>No es una via de escalada: para llegar aqui hay que partir de un token valido,
     * y ese atacante ya tenia acceso. Importaria si algun dia se anade una lista de
     * revocacion indexada por la cadena del token, porque dos cadenas distintas
     * representarian la misma sesion.
     */
    @Test
    void un_caracter_suelto_al_final_no_cambia_la_identidad_pero_dos_si() {
        var service = serviceAt(NOW);
        String token = service.issue(user).accessToken();

        assertThat(service.resolveUserId(token + "x")).contains(user.id());
        assertThat(service.resolveUserId(token + "xy")).isEmpty();
    }

    @Test
    void un_token_firmado_con_otra_clave_se_rechaza() {
        var otherIssuer = new JwtService(
                new JwtProperties("otra-clave-igual-de-larga-pero-distinta-0987654321",
                        Duration.ofMinutes(30), Duration.ofDays(30), "habit-tracker"),
                Clock.fixed(NOW, ZoneOffset.UTC));

        String foreignToken = otherIssuer.issue(user).accessToken();

        assertThat(serviceAt(NOW).resolveUserId(foreignToken)).isEmpty();
    }

    @Test
    void un_token_caducado_se_rechaza() {
        String token = serviceAt(NOW).issue(user).accessToken();

        // Media hora de TTL: una hora despues ya no vale.
        var later = serviceAt(NOW.plus(Duration.ofHours(1)));

        assertThat(later.resolveUserId(token)).isEmpty();
    }

    @Test
    void basura_que_no_es_un_jwt_se_rechaza_sin_explotar() {
        var service = serviceAt(NOW);

        assertThat(service.resolveUserId("")).isEmpty();
        assertThat(service.resolveUserId("no-es-un-token")).isEmpty();
        assertThat(service.resolveUserId("a.b.c")).isEmpty();
    }
}
