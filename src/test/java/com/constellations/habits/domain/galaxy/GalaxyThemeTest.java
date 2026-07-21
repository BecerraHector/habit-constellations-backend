package com.constellations.habits.domain.galaxy;

import com.constellations.habits.domain.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GalaxyThemeTest {

    @Test
    void variantes_de_lo_mismo_caen_en_el_mismo_tema() {
        // Si no se agruparan, el catalogo mostraria "Gym", "gym" y "GYM " como tres
        // temas distintos y ninguno pareceria popular.
        var gym = new GalaxyTheme("gym");

        assertThat(new GalaxyTheme("Gym")).isEqualTo(gym);
        assertThat(new GalaxyTheme("  GYM  ")).isEqualTo(gym);
    }

    @Test
    void las_tildes_no_parten_un_tema_en_dos() {
        assertThat(new GalaxyTheme("meditación")).isEqualTo(new GalaxyTheme("meditacion"));
    }

    @Test
    void los_espacios_se_vuelven_guiones() {
        assertThat(new GalaxyTheme("dormir 7 8 horas").value()).isEqualTo("dormir-7-8-horas");
        assertThat(new GalaxyTheme("leer  antes   de dormir").value())
                .isEqualTo("leer-antes-de-dormir");
    }

    @Test
    void los_signos_se_descartan_sin_dejar_guiones_sueltos() {
        assertThat(new GalaxyTheme("¡Gym!").value()).isEqualTo("gym");
        assertThat(new GalaxyTheme("-- correr --").value()).isEqualTo("correr");
    }

    @Test
    void un_tema_sin_letras_ni_numeros_se_rechaza() {
        assertThatThrownBy(() -> new GalaxyTheme("!!!")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new GalaxyTheme("   ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new GalaxyTheme(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void las_sugerencias_ya_estan_normalizadas() {
        GalaxyTheme.SUGGESTED.forEach(theme ->
                assertThat(theme.value()).isEqualTo(GalaxyTheme.normalize(theme.value())));
    }
}
