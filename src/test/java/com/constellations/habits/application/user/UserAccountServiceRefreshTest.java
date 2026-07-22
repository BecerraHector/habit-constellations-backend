package com.constellations.habits.application.user;

import com.constellations.habits.application.exception.InvalidRefreshTokenException;
import com.constellations.habits.application.galaxy.GalaxyService;
import com.constellations.habits.application.port.out.AccessTokenIssuer;
import com.constellations.habits.application.port.out.FriendshipRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.LoginAttemptLimiter;
import com.constellations.habits.application.port.out.PasswordHasher;
import com.constellations.habits.application.port.out.RefreshTokenRepository;
import com.constellations.habits.application.port.out.TokenHasher;
import com.constellations.habits.application.port.out.TransactionRunner;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.user.InviteCode;
import com.constellations.habits.domain.user.RefreshToken;
import com.constellations.habits.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La rotacion del token de refresco y la deteccion de reutilizacion, con dobles
 * (PENDIENTES.md, punto 3): los dos comportamientos de seguridad mas delicados del
 * servicio, probados sin HTTP ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAccountServiceRefreshTest {

    private static final Instant NOW = Instant.parse("2026-07-22T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration TTL = Duration.ofDays(30);

    @Mock UserRepository users;
    @Mock HabitRepository habits;
    @Mock FriendshipRepository friendships;
    @Mock GalaxyService galaxies;
    @Mock PasswordHasher hasher;
    @Mock AccessTokenIssuer tokens;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock TokenHasher tokenHasher;
    @Mock InviteCodeAllocator inviteCodes;
    @Mock TransactionRunner transaction;
    @Mock LoginAttemptLimiter loginAttempts;

    private UserAccountService service;
    private User ana;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(
                users, habits, friendships, galaxies, hasher, tokens, refreshTokens,
                tokenHasher, inviteCodes, transaction, loginAttempts, TTL, CLOCK);

        ana = User.register(
                "ana@test.dev", "$hash$", "Ana", ZoneId.of("America/Lima"),
                new InviteCode("ABCD2345"), NOW);

        // El hash es determinista y prefijado: permite predecir la busqueda por hash.
        when(tokenHasher.hash(any())).thenAnswer(inv -> "h:" + inv.getArgument(0));
        when(transaction.execute(any())).thenAnswer(inv ->
                ((Supplier<?>) inv.getArgument(0)).get());
        when(users.findById(ana.id())).thenReturn(Optional.of(ana));
        when(tokens.issue(ana)).thenReturn(
                new AccessTokenIssuer.IssuedToken("un-jwt", Duration.ofMinutes(30)));
        when(refreshTokens.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void un_refresco_valido_rota_el_token() {
        RefreshToken stored = RefreshToken.issue(
                ana.id(), "h:token-vivo", NOW.minus(Duration.ofDays(1)), TTL);
        when(refreshTokens.findByHash("h:token-vivo")).thenReturn(Optional.of(stored));

        AuthenticatedUser renewed = service.refresh("token-vivo");

        // Se entrega uno nuevo, distinto del presentado.
        assertThat(renewed.refreshToken()).isNotEqualTo("token-vivo");

        // Y el presentado queda revocado en la misma unidad de trabajo.
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .anySatisfy(token -> {
                    assertThat(token.id()).isEqualTo(stored.id());
                    assertThat(token.isRevoked()).isTrue();
                });
        verify(transaction).execute(any());
    }

    @Test
    void presentar_un_token_ya_revocado_cierra_todas_las_sesiones() {
        RefreshToken revoked = RefreshToken.issue(
                        ana.id(), "h:token-robado", NOW.minus(Duration.ofDays(1)), TTL)
                .revoke(NOW.minus(Duration.ofHours(1)));
        when(refreshTokens.findByHash("h:token-robado")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("token-robado"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        // El cierre masivo ocurre fuera de transaccion: debe sobrevivir a la excepcion.
        verify(refreshTokens).revokeAllForUser(ana.id(), NOW);
        verify(transaction, never()).execute(any());
    }

    @Test
    void un_token_caducado_se_rechaza_sin_cerrar_nada_mas() {
        RefreshToken expired = RefreshToken.issue(
                ana.id(), "h:token-caducado", NOW.minus(Duration.ofDays(40)), TTL);
        when(refreshTokens.findByHash("h:token-caducado")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("token-caducado"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        // Caducar es lo esperado con el tiempo; no es senal de robo.
        verify(refreshTokens, never()).revokeAllForUser(any(), any());
    }

    @Test
    void un_token_desconocido_se_rechaza_sin_distinguir_el_motivo() {
        when(refreshTokens.findByHash("h:inventado")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("inventado"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void un_token_en_blanco_ni_siquiera_se_busca() {
        assertThatThrownBy(() -> service.refresh("  "))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokens, never()).findByHash(any());
    }
}
