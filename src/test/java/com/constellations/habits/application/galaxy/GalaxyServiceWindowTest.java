package com.constellations.habits.application.galaxy;

import com.constellations.habits.application.port.out.GalaxyMembershipRepository;
import com.constellations.habits.application.port.out.GalaxyRepository;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.TransactionRunner;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.galaxy.Galaxy;
import com.constellations.habits.domain.galaxy.GalaxyMembership;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * La ventana del mapa de brillo, con los puertos doblados (PENDIENTES.md, punto 3).
 * Todo gira sobre un reloj fijo: son justo los casos que la integracion encarece.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GalaxyServiceWindowTest {

    /** 12:00 UTC = 07:00 en Lima: para este usuario, hoy es el 22 de julio. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.parse("2026-07-22");

    @Mock GalaxyRepository galaxies;
    @Mock GalaxyMembershipRepository memberships;
    @Mock HabitRepository habits;
    @Mock HabitLogRepository logs;
    @Mock UserRepository users;
    @Mock TransactionRunner transaction;

    private GalaxyService service;
    private User ana;
    private Galaxy galaxy;

    @BeforeEach
    void setUp() {
        service = new GalaxyService(galaxies, memberships, habits, logs, users, transaction, CLOCK);

        ana = User.register(
                "ana@test.dev", "$hash$", "Ana", ZoneId.of("America/Lima"),
                new InviteCode("ABCD2345"), CLOCK.instant());
        galaxy = Galaxy.create(ana.id(), "Lectores del alba", null, "lectura", CLOCK.instant());

        when(users.findById(ana.id())).thenReturn(Optional.of(ana));
        when(galaxies.findById(galaxy.id())).thenReturn(Optional.of(galaxy));
        when(logs.findDatesByHabitsBetween(anyList(), any(), any())).thenReturn(Map.of());
    }

    @Test
    void la_ventana_por_defecto_cubre_el_ciclo_de_30_dias() {
        memberSince(TODAY.minusDays(100));

        GalaxyDetail detail = service.get(ana.id(), galaxy.id(), null);

        assertThat(detail.map().from()).isEqualTo(TODAY.minusDays(29));
        assertThat(detail.map().to()).isEqualTo(TODAY);
        assertThat(detail.map().days()).hasSize(30);
    }

    @Test
    void el_mapa_no_empieza_antes_del_primer_miembro() {
        // Galaxia de cinco dias: pintar los 25 anteriores seria una franja sin sentido.
        memberSince(TODAY.minusDays(4));

        GalaxyDetail detail = service.get(ana.id(), galaxy.id(), null);

        assertThat(detail.map().from()).isEqualTo(TODAY.minusDays(4));
        assertThat(detail.map().days()).hasSize(5);
    }

    @Test
    void manda_el_miembro_mas_antiguo_aunque_ya_se_haya_ido() {
        UUID otherUser = UUID.randomUUID();
        GalaxyMembership veteranGone = GalaxyMembership.join(
                        galaxy.id(), otherUser, UUID.randomUUID(),
                        TODAY.minusDays(10), CLOCK.instant())
                .leave(TODAY.minusDays(2), CLOCK.instant());
        GalaxyMembership recent = GalaxyMembership.join(
                galaxy.id(), ana.id(), UUID.randomUUID(), TODAY.minusDays(3), CLOCK.instant());

        when(memberships.findAllByGalaxy(galaxy.id())).thenReturn(List.of(veteranGone, recent));

        GalaxyDetail detail = service.get(ana.id(), galaxy.id(), null);

        // Quien se fue sigue definiendo desde cuando existe historia que pintar.
        assertThat(detail.map().from()).isEqualTo(TODAY.minusDays(10));
    }

    @Test
    void una_ventana_pedida_mas_grande_que_el_tope_se_recorta() {
        memberSince(TODAY.minusDays(1000));

        GalaxyDetail detail = service.get(ana.id(), galaxy.id(), 1000);

        assertThat(detail.map().from()).isEqualTo(TODAY.minusDays(GalaxyService.MAX_WINDOW_DAYS - 1L));
    }

    @Test
    void una_ventana_invalida_cae_al_defecto() {
        memberSince(TODAY.minusDays(100));

        GalaxyDetail detail = service.get(ana.id(), galaxy.id(), -5);

        assertThat(detail.map().from()).isEqualTo(TODAY.minusDays(29));
    }

    private void memberSince(LocalDate joinedOn) {
        GalaxyMembership membership = GalaxyMembership.join(
                galaxy.id(), ana.id(), UUID.randomUUID(), joinedOn, CLOCK.instant());
        when(memberships.findAllByGalaxy(galaxy.id())).thenReturn(List.of(membership));
    }
}
