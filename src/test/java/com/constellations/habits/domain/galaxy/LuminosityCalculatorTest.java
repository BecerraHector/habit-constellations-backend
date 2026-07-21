package com.constellations.habits.domain.galaxy;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LuminosityCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-07-21T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 21);
    private static final UUID GALAXY = UUID.randomUUID();

    @Test
    void una_estrella_se_apaga_cuando_nadie_cumple_pero_no_desaparece() {
        var ana = member(TODAY.minusDays(10), null);
        var bruno = member(TODAY.minusDays(10), null);

        GalaxyMap map = LuminosityCalculator.map(
                List.of(ana, bruno), Map.of(), TODAY, TODAY);

        GalaxyDay day = map.days().get(0);
        assertThat(day.level()).isEqualTo(Luminosity.DARK);
        assertThat(day.activeMembers()).isEqualTo(2);
        assertThat(day.completions()).isZero();
    }

    @Test
    void el_brillo_maximo_se_reserva_al_pleno() {
        var ana = member(TODAY.minusDays(1), null);
        var bruno = member(TODAY.minusDays(1), null);
        var carla = member(TODAY.minusDays(1), null);

        // Dos de tres brillan, pero no como los tres de tres: "hoy cumplimos todos" es
        // el unico dia que merece distinguirse de un vistazo.
        var parcial = LuminosityCalculator.map(
                List.of(ana, bruno, carla),
                Map.of(ana.habitId(), Set.of(TODAY), bruno.habitId(), Set.of(TODAY)),
                TODAY, TODAY);

        var pleno = LuminosityCalculator.map(
                List.of(ana, bruno, carla),
                Map.of(
                        ana.habitId(), Set.of(TODAY),
                        bruno.habitId(), Set.of(TODAY),
                        carla.habitId(), Set.of(TODAY)),
                TODAY, TODAY);

        assertThat(parcial.days().get(0).level()).isEqualTo(3);
        assertThat(pleno.days().get(0).level()).isEqualTo(Luminosity.MAX_LEVEL);
        assertThat(pleno.days().get(0).isPerfect()).isTrue();
    }

    @Test
    void quien_no_habia_llegado_no_cuenta_en_el_denominador() {
        var veterana = member(TODAY.minusDays(5), null);
        var recien = member(TODAY, null);

        // La veterana cumplio ayer, cuando estaba sola: ese dia fue un pleno y debe
        // seguir siendolo aunque hoy el grupo sea el doble de grande.
        GalaxyMap map = LuminosityCalculator.map(
                List.of(veterana, recien),
                Map.of(veterana.habitId(), Set.of(TODAY.minusDays(1))),
                TODAY.minusDays(1), TODAY);

        GalaxyDay ayer = map.days().get(0);
        assertThat(ayer.activeMembers()).isEqualTo(1);
        assertThat(ayer.isPerfect()).isTrue();

        GalaxyDay hoy = map.days().get(1);
        assertThat(hoy.activeMembers()).isEqualTo(2);
        assertThat(hoy.level()).isEqualTo(Luminosity.DARK);
    }

    @Test
    void quien_se_fue_deja_de_pesar_el_dia_que_se_va() {
        var quedan = member(TODAY.minusDays(5), null);
        var seFue = member(TODAY.minusDays(5), TODAY);

        GalaxyMap map = LuminosityCalculator.map(
                List.of(quedan, seFue),
                Map.of(quedan.habitId(), Set.of(TODAY, TODAY.minusDays(1))),
                TODAY.minusDays(1), TODAY);

        // Ayer aun eran dos y solo cumplio uno.
        assertThat(map.days().get(0).activeMembers()).isEqualTo(2);
        assertThat(map.days().get(0).isPerfect()).isFalse();

        // Hoy queda uno, y su cumplimiento ya es un pleno: la ausencia de quien se
        // marcho no debe ensombrecer un dia del que ya no forma parte.
        assertThat(map.days().get(1).activeMembers()).isEqualTo(1);
        assertThat(map.days().get(1).isPerfect()).isTrue();
    }

    @Test
    void las_estrellas_de_quien_ya_no_esta_no_las_hereda_el_grupo() {
        var seFue = member(TODAY.minusDays(5), TODAY);

        // Sigue con el habito por su cuenta y lo marca hoy, ya fuera de la galaxia.
        GalaxyMap map = LuminosityCalculator.map(
                List.of(seFue), Map.of(seFue.habitId(), Set.of(TODAY)), TODAY, TODAY);

        GalaxyDay day = map.days().get(0);
        assertThat(day.activeMembers()).isZero();
        assertThat(day.completions()).isZero();
        assertThat(day.level()).isEqualTo(Luminosity.DARK);
    }

    @Test
    void la_ventana_cubre_todos_los_dias_incluidos_los_extremos() {
        var ana = member(TODAY.minusDays(30), null);

        GalaxyMap map = LuminosityCalculator.map(
                List.of(ana), Map.of(), TODAY.minusDays(29), TODAY);

        assertThat(map.days()).hasSize(30);
        assertThat(map.days().get(0).date()).isEqualTo(TODAY.minusDays(29));
        assertThat(map.days().get(29).date()).isEqualTo(TODAY);
    }

    @Test
    void una_ventana_invertida_no_explota() {
        assertThat(LuminosityCalculator.map(List.of(), Map.of(), TODAY, TODAY.minusDays(1)))
                .isEqualTo(GalaxyMap.EMPTY);
    }

    @Test
    void los_agregados_resumen_la_ventana_sin_hablar_de_rachas() {
        var ana = member(TODAY.minusDays(3), null);
        var bruno = member(TODAY.minusDays(3), null);

        GalaxyMap map = LuminosityCalculator.map(
                List.of(ana, bruno),
                Map.of(
                        ana.habitId(), Set.of(TODAY, TODAY.minusDays(1)),
                        bruno.habitId(), Set.of(TODAY)),
                TODAY.minusDays(1), TODAY);

        assertThat(map.totalStars()).isEqualTo(3);
        assertThat(map.perfectDays()).isEqualTo(1);
        // Un dia al 50% y otro al 100%.
        assertThat(map.averageRatio()).isEqualTo(0.75);
    }

    @Test
    void un_solo_miembro_solo_puede_estar_apagado_o_al_maximo() {
        assertThat(Luminosity.levelOf(0, 1)).isEqualTo(Luminosity.DARK);
        assertThat(Luminosity.levelOf(1, 1)).isEqualTo(Luminosity.MAX_LEVEL);
    }

    @Test
    void la_escala_reparte_los_tramos_intermedios() {
        assertThat(Luminosity.levelOf(1, 8)).isEqualTo(1);
        assertThat(Luminosity.levelOf(2, 8)).isEqualTo(1);
        assertThat(Luminosity.levelOf(3, 8)).isEqualTo(2);
        assertThat(Luminosity.levelOf(4, 8)).isEqualTo(2);
        assertThat(Luminosity.levelOf(5, 8)).isEqualTo(3);
        assertThat(Luminosity.levelOf(7, 8)).isEqualTo(3);
        assertThat(Luminosity.levelOf(8, 8)).isEqualTo(Luminosity.MAX_LEVEL);
    }

    @Test
    void una_galaxia_sin_miembros_ese_dia_no_divide_entre_cero() {
        assertThat(Luminosity.levelOf(0, 0)).isEqualTo(Luminosity.DARK);
        assertThat(new GalaxyDay(TODAY, 0, 0, 0).ratio()).isZero();
    }

    private static GalaxyMembership member(LocalDate joinedOn, LocalDate leftOn) {
        return new GalaxyMembership(
                UUID.randomUUID(),
                GALAXY,
                UUID.randomUUID(),
                UUID.randomUUID(),
                joinedOn,
                NOW,
                leftOn,
                leftOn == null ? null : NOW);
    }
}
