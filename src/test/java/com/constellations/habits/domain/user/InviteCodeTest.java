package com.constellations.habits.domain.user;

import com.constellations.habits.domain.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InviteCodeTest {

    @Test
    void se_genera_con_la_longitud_y_el_formato_esperados() {
        var code = InviteCode.generate();

        assertThat(code.value()).hasSize(8).matches("[A-Z2-9]+");
        assertThat(code.formatted()).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
    }

    @Test
    void nunca_incluye_caracteres_ambiguos() {
        // O/0 e I/1 se confunden al dictar el codigo en voz alta o al teclearlo.
        var random = new Random(7);

        for (int i = 0; i < 500; i++) {
            assertThat(InviteCode.generate(random).value()).doesNotContain("O", "0", "I", "1");
        }
    }

    @Test
    void genera_codigos_distintos() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            generated.add(InviteCode.generate().value());
        }

        assertThat(generated).hasSize(200);
    }

    @Test
    void acepta_el_codigo_tal_y_como_lo_pegue_el_usuario() {
        var canonical = new InviteCode("ABCD2345");

        assertThat(new InviteCode("abcd-2345")).isEqualTo(canonical);
        assertThat(new InviteCode("  ABCD 2345 ")).isEqualTo(canonical);
        assertThat(new InviteCode("ABCD-2345")).isEqualTo(canonical);
    }

    @Test
    void rechaza_longitudes_incorrectas() {
        assertThatThrownBy(() -> new InviteCode("ABCD234")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new InviteCode("ABCD23456")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new InviteCode("")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rechaza_caracteres_fuera_del_alfabeto() {
        assertThatThrownBy(() -> new InviteCode("ABCD230O")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new InviteCode("ABCD23!5")).isInstanceOf(ValidationException.class);
    }
}
