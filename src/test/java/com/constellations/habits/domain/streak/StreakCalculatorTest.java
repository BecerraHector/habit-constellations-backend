package com.constellations.habits.domain.streak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 21);

    /** Los n dias consecutivos que terminan en {@code last}, incluido. */
    private static List<LocalDate> consecutiveEndingAt(LocalDate last, int n) {
        return IntStream.range(0, n).mapToObj(i -> last.minusDays(i)).toList();
    }

    @Nested
    @DisplayName("racha actual")
    class CurrentStreak {

        @Test
        void es_cero_sin_ningun_log() {
            assertThat(StreakCalculator.calculate(List.of(), TODAY)).isEqualTo(HabitProgress.EMPTY);
        }

        @Test
        void cuenta_los_dias_consecutivos_que_terminan_hoy() {
            var progress = StreakCalculator.calculate(consecutiveEndingAt(TODAY, 5), TODAY);

            assertThat(progress.currentStreak()).isEqualTo(5);
            assertThat(progress.completedToday()).isTrue();
        }

        @Test
        void sigue_viva_si_el_ultimo_cumplimiento_fue_ayer() {
            // Aun no ha marcado hoy, pero le queda el dia entero para hacerlo.
            var progress = StreakCalculator.calculate(consecutiveEndingAt(TODAY.minusDays(1), 4), TODAY);

            assertThat(progress.currentStreak()).isEqualTo(4);
            assertThat(progress.completedToday()).isFalse();
        }

        @Test
        void se_pierde_si_el_ultimo_cumplimiento_fue_anteayer() {
            var progress = StreakCalculator.calculate(consecutiveEndingAt(TODAY.minusDays(2), 10), TODAY);

            assertThat(progress.currentStreak()).isZero();
            assertThat(progress.longestStreak()).isEqualTo(10);
        }

        @Test
        void un_solo_dia_fallado_reinicia_el_contador() {
            // Cumple 3 dias, falla el cuarto, y vuelve a cumplir 2 hasta hoy.
            var days = new java.util.ArrayList<>(consecutiveEndingAt(TODAY, 2));
            days.addAll(consecutiveEndingAt(TODAY.minusDays(3), 3));

            var progress = StreakCalculator.calculate(days, TODAY);

            assertThat(progress.currentStreak()).isEqualTo(2);
            assertThat(progress.longestStreak()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("robustez de la entrada")
    class InputHandling {

        @Test
        void ignora_fechas_duplicadas() {
            var days = List.of(TODAY, TODAY, TODAY.minusDays(1), TODAY.minusDays(1));

            var progress = StreakCalculator.calculate(days, TODAY);

            assertThat(progress.currentStreak()).isEqualTo(2);
            assertThat(progress.totalCompletions()).isEqualTo(2);
        }

        @Test
        void no_depende_del_orden_de_llegada() {
            var ordered = consecutiveEndingAt(TODAY, 7);
            var shuffled = new java.util.ArrayList<>(ordered);
            java.util.Collections.shuffle(shuffled, new java.util.Random(42));

            assertThat(StreakCalculator.calculate(shuffled, TODAY))
                    .isEqualTo(StreakCalculator.calculate(ordered, TODAY));
        }

        @Test
        void cruza_correctamente_el_cambio_de_mes() {
            var last = LocalDate.of(2026, 3, 2);
            // Marzo 2 hacia atras: pasa por el 1 de marzo y el 28 de febrero (2026 no es bisiesto).
            var progress = StreakCalculator.calculate(consecutiveEndingAt(last, 4), last);

            assertThat(progress.currentStreak()).isEqualTo(4);
            assertThat(progress.lastCompleted()).contains(last);
        }
    }

    @Nested
    @DisplayName("constelaciones")
    class Constellations {

        @Test
        void no_hay_ninguna_antes_de_completar_el_ciclo() {
            int justUnder = StreakCalculator.CYCLE_LENGTH - 1;

            var progress = StreakCalculator.calculate(consecutiveEndingAt(TODAY, justUnder), TODAY);

            assertThat(progress.completedConstellations()).isZero();
            assertThat(progress.starsInCurrentCycle()).isEqualTo(justUnder);
            assertThat(progress.daysToNextConstellation()).isEqualTo(1);
        }

        @Test
        void se_cierra_una_al_completar_el_ciclo() {
            var progress = StreakCalculator.calculate(
                    consecutiveEndingAt(TODAY, StreakCalculator.CYCLE_LENGTH), TODAY);

            assertThat(progress.completedConstellations()).isEqualTo(1);
            assertThat(progress.starsInCurrentCycle()).isZero();
        }

        @Test
        void una_racha_larga_acumula_varias() {
            int twoCyclesPlusFive = StreakCalculator.CYCLE_LENGTH * 2 + 5;

            var progress = StreakCalculator.calculate(consecutiveEndingAt(TODAY, twoCyclesPlusFive), TODAY);

            assertThat(progress.completedConstellations()).isEqualTo(2);
            assertThat(progress.starsInCurrentCycle()).isEqualTo(5);
        }

        @Test
        void las_ya_ganadas_sobreviven_a_romper_la_racha() {
            // Cerro una constelacion, la racha murio hace tiempo, pero el logro es historia.
            var old = consecutiveEndingAt(TODAY.minusDays(10), StreakCalculator.CYCLE_LENGTH);

            var progress = StreakCalculator.calculate(old, TODAY);

            assertThat(progress.currentStreak()).isZero();
            assertThat(progress.completedConstellations()).isEqualTo(1);
        }
    }
}
