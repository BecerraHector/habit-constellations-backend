package com.constellations.habits.domain.streak;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El denominador del mapa personal: que habitos pesan cada dia. Espeja las reglas de
 * pertenencia de una galaxia, pero sobre la vida de los habitos propios.
 */
class SkyCalculatorTest {

    private static final LocalDate DAY = LocalDate.parse("2026-07-15");

    private final UUID leer = UUID.randomUUID();
    private final UUID meditar = UUID.randomUUID();

    @Test
    void antes_de_crearse_un_habito_no_pesa_en_el_denominador() {
        var spans = List.of(new HabitSpan(leer, DAY, null));

        var days = SkyCalculator.map(spans, Map.of(), DAY.minusDays(1), DAY);

        assertThat(days.get(0).activeHabits()).isZero();
        assertThat(days.get(0).level()).isZero();
        assertThat(days.get(1).activeHabits()).isEqualTo(1);
    }

    @Test
    void la_estrella_rellenada_de_ayer_brilla_aunque_el_habito_se_creara_hoy() {
        // La ventana de repaso permite rellenar ayer en un habito recien creado; esa
        // estrella existe y el mapa debe ensenarla.
        var spans = List.of(new HabitSpan(leer, DAY, null));

        var days = SkyCalculator.map(
                spans, Map.of(leer, Set.of(DAY.minusDays(1))), DAY.minusDays(1), DAY);

        assertThat(days.get(0).activeHabits()).isEqualTo(1);
        assertThat(days.get(0).isPerfect()).isTrue();
    }

    @Test
    void el_nivel_es_proporcional_y_el_maximo_se_reserva_al_pleno() {
        var spans = List.of(
                new HabitSpan(leer, DAY.minusDays(30), null),
                new HabitSpan(meditar, DAY.minusDays(30), null));
        var completions = Map.of(
                leer, Set.of(DAY.minusDays(1), DAY),
                meditar, Set.of(DAY));

        var days = SkyCalculator.map(spans, completions, DAY.minusDays(2), DAY);

        // Nadie cumplio: apagada pero presente.
        assertThat(days.get(0).level()).isZero();
        // 1 de 2: medio cielo.
        assertThat(days.get(1).completions()).isEqualTo(1);
        assertThat(days.get(1).level()).isEqualTo(2);
        // 2 de 2: pleno.
        assertThat(days.get(2).isPerfect()).isTrue();
        assertThat(days.get(2).level()).isEqualTo(4);
    }

    @Test
    void el_dia_del_archivo_cuenta_solo_si_se_cumplio() {
        var spans = List.of(
                new HabitSpan(leer, DAY.minusDays(30), DAY),
                new HabitSpan(meditar, DAY.minusDays(30), null));

        // Sin cumplimiento el dia del archivo: el habito no pesa y no apunta un fallo.
        var sinCumplir = SkyCalculator.map(spans, Map.of(meditar, Set.of(DAY)), DAY, DAY);
        assertThat(sinCumplir.get(0).activeHabits()).isEqualTo(1);
        assertThat(sinCumplir.get(0).isPerfect()).isTrue();

        // Con cumplimiento: la estrella ganada por la manana no se borra por la tarde.
        var cumpliendo = SkyCalculator.map(
                spans, Map.of(leer, Set.of(DAY), meditar, Set.of(DAY)), DAY, DAY);
        assertThat(cumpliendo.get(0).activeHabits()).isEqualTo(2);
        assertThat(cumpliendo.get(0).completions()).isEqualTo(2);
    }

    @Test
    void despues_del_archivo_el_habito_ya_no_oscurece_nada() {
        var spans = List.of(
                new HabitSpan(leer, DAY.minusDays(30), DAY.minusDays(5)),
                new HabitSpan(meditar, DAY.minusDays(30), null));

        var days = SkyCalculator.map(spans, Map.of(meditar, Set.of(DAY)), DAY, DAY);

        assertThat(days.get(0).activeHabits()).isEqualTo(1);
        assertThat(days.get(0).isPerfect()).isTrue();
    }

    @Test
    void una_ventana_invalida_no_pinta_nada() {
        var spans = List.of(new HabitSpan(leer, DAY.minusDays(30), null));

        assertThat(SkyCalculator.map(spans, Map.of(), DAY, DAY.minusDays(1))).isEmpty();
        assertThat(SkyCalculator.map(spans, Map.of(), null, DAY)).isEmpty();
        assertThat(SkyCalculator.map(spans, Map.of(), DAY, null)).isEmpty();
    }
}
