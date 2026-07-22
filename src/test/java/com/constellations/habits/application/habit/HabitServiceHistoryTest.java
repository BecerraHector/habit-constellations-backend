package com.constellations.habits.application.habit;

import com.constellations.habits.application.exception.HabitNotFoundException;
import com.constellations.habits.application.galaxy.GalaxyService;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.TransactionRunner;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.user.InviteCode;
import com.constellations.habits.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La ventana del historial de un habito, con los puertos doblados. Misma filosofia que
 * la del mapa de brillo: valores ausentes o invalidos caen al defecto, nunca fallan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HabitServiceHistoryTest {

    /** 12:00 UTC = 07:00 en Lima: para esta usuaria, hoy es el 22 de julio. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.parse("2026-07-22");

    @Mock HabitRepository habits;
    @Mock HabitLogRepository logs;
    @Mock UserRepository users;
    @Mock GalaxyService galaxies;
    @Mock TransactionRunner transaction;

    private HabitService service;
    private User ana;
    private Habit habit;

    @BeforeEach
    void setUp() {
        service = new HabitService(habits, logs, users, galaxies, transaction, CLOCK);

        ana = User.register(
                "ana@test.dev", "$hash$", "Ana", ZoneId.of("America/Lima"),
                new InviteCode("ABCD2345"), CLOCK.instant());
        habit = Habit.create(ana.id(), "Leer 20 minutos", null, CLOCK.instant());

        when(users.findById(ana.id())).thenReturn(Optional.of(ana));
        when(habits.findById(habit.id())).thenReturn(Optional.of(habit));
        when(logs.findDatesByHabitsBetween(anyList(), any(), any())).thenReturn(Map.of());
    }

    @Test
    void sin_parametros_la_ventana_son_los_ultimos_90_dias() {
        HabitHistory history = service.history(ana.id(), habit.id(), null, null);

        assertThat(history.from()).isEqualTo(TODAY.minusDays(89));
        assertThat(history.to()).isEqualTo(TODAY);
        verify(logs).findDatesByHabitsBetween(
                eq(List.of(habit.id())), eq(TODAY.minusDays(89)), eq(TODAY));
    }

    @Test
    void un_final_en_el_futuro_se_recorta_al_hoy_del_usuario() {
        HabitHistory history =
                service.history(ana.id(), habit.id(), TODAY.minusDays(5), TODAY.plusDays(30));

        assertThat(history.from()).isEqualTo(TODAY.minusDays(5));
        assertThat(history.to()).isEqualTo(TODAY);
    }

    @Test
    void un_inicio_posterior_al_final_cae_a_la_ventana_por_defecto() {
        LocalDate end = TODAY.minusDays(10);

        HabitHistory history = service.history(ana.id(), habit.id(), TODAY.minusDays(2), end);

        assertThat(history.from()).isEqualTo(end.minusDays(89));
        assertThat(history.to()).isEqualTo(end);
    }

    @Test
    void la_ventana_nunca_supera_el_ano() {
        HabitHistory history =
                service.history(ana.id(), habit.id(), TODAY.minusDays(3000), TODAY);

        assertThat(history.from()).isEqualTo(TODAY.minusDays(365));
        assertThat(history.to()).isEqualTo(TODAY);
    }

    @Test
    void las_fechas_llegan_ordenadas_aunque_el_puerto_no_las_ordene() {
        when(logs.findDatesByHabitsBetween(anyList(), any(), any())).thenReturn(Map.of(
                habit.id(),
                List.of(TODAY, TODAY.minusDays(4), TODAY.minusDays(1))));

        HabitHistory history = service.history(ana.id(), habit.id(), null, null);

        assertThat(history.dates())
                .containsExactly(TODAY.minusDays(4), TODAY.minusDays(1), TODAY);
    }

    @Test
    void el_historial_de_un_habito_ajeno_se_reporta_como_inexistente() {
        UUID intrusa = UUID.randomUUID();
        when(users.findById(intrusa)).thenReturn(Optional.of(User.register(
                "eva@test.dev", "$hash$", "Eva", ZoneId.of("America/Lima"),
                new InviteCode("WXYZ7892"), CLOCK.instant())));

        assertThatThrownBy(() -> service.history(intrusa, habit.id(), null, null))
                .isInstanceOf(HabitNotFoundException.class);
    }
}
