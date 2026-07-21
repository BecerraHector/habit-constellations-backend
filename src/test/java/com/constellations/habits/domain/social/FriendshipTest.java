package com.constellations.habits.domain.social;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendshipTest {

    private static final Instant NOW = Instant.parse("2026-07-21T10:00:00Z");
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BRUNO = UUID.randomUUID();
    private static final UUID CARLA = UUID.randomUUID();

    @Test
    void una_solicitud_nace_pendiente() {
        var request = Friendship.request(ANA, BRUNO, NOW);

        assertThat(request.isPending()).isTrue();
        assertThat(request.isAccepted()).isFalse();
        assertThat(request.respondedAt()).isNull();
    }

    @Test
    void nadie_puede_enviarse_una_solicitud_a_si_mismo() {
        assertThatThrownBy(() -> Friendship.request(ANA, ANA, NOW))
                .isInstanceOf(SelfFriendshipException.class);
    }

    @Test
    void el_destinatario_puede_aceptar() {
        var accepted = Friendship.request(ANA, BRUNO, NOW).accept(BRUNO, NOW);

        assertThat(accepted.isAccepted()).isTrue();
        assertThat(accepted.respondedAt()).isEqualTo(NOW);
    }

    @Test
    void el_solicitante_no_puede_aceptar_su_propia_solicitud() {
        var request = Friendship.request(ANA, BRUNO, NOW);

        // Si pudiera, el consentimiento del destinatario no significaria nada.
        assertThatThrownBy(() -> request.accept(ANA, NOW))
                .isInstanceOf(FriendshipStateException.class);
    }

    @Test
    void un_tercero_no_puede_responder() {
        var request = Friendship.request(ANA, BRUNO, NOW);

        assertThatThrownBy(() -> request.accept(CARLA, NOW))
                .isInstanceOf(FriendshipStateException.class);
    }

    @Test
    void una_solicitud_ya_respondida_no_se_puede_volver_a_responder() {
        var accepted = Friendship.request(ANA, BRUNO, NOW).accept(BRUNO, NOW);

        assertThatThrownBy(() -> accepted.decline(BRUNO, NOW))
                .isInstanceOf(FriendshipStateException.class);
    }

    @Test
    void la_relacion_es_simetrica_una_vez_creada() {
        var request = Friendship.request(ANA, BRUNO, NOW);

        assertThat(request.involves(ANA)).isTrue();
        assertThat(request.involves(BRUNO)).isTrue();
        assertThat(request.involves(CARLA)).isFalse();
        assertThat(request.otherParty(ANA)).isEqualTo(BRUNO);
        assertThat(request.otherParty(BRUNO)).isEqualTo(ANA);
    }

    @Test
    void rechazar_conserva_la_relacion_en_estado_declinado() {
        // Se conserva a proposito: borrarla permitiria reenviar la solicitud en bucle.
        var declined = Friendship.request(ANA, BRUNO, NOW).decline(BRUNO, NOW);

        assertThat(declined.status()).isEqualTo(FriendshipStatus.DECLINED);
        assertThat(declined.isPending()).isFalse();
    }
}
