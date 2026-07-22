package com.constellations.habits.application.user;

import com.constellations.habits.application.exception.InvalidCredentialsException;
import com.constellations.habits.application.exception.TooManyAttemptsException;
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
import com.constellations.habits.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Primer test unitario de la capa de aplicacion (PENDIENTES.md, punto 3): los puertos
 * se doblan y el caso de uso se ejercita sin HTTP, sin Spring y sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceLoginTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);

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

    @BeforeEach
    void setUp() {
        service = new UserAccountService(
                users, habits, friendships, galaxies, hasher, tokens, refreshTokens,
                tokenHasher, inviteCodes, transaction, loginAttempts,
                Duration.ofDays(30), CLOCK);
    }

    @Test
    void una_cuenta_frenada_ni_siquiera_consulta_la_base() {
        when(loginAttempts.retryAfter("ana@test.dev")).thenReturn(Duration.ofSeconds(30));

        assertThatThrownBy(() -> service.login(new LoginCommand("Ana@Test.dev", "lo-que-sea")))
                .isInstanceOf(TooManyAttemptsException.class);

        verifyNoInteractions(users);
        verify(loginAttempts, never()).recordFailure(anyString());
    }

    @Test
    void un_fallo_de_credenciales_queda_anotado() {
        when(loginAttempts.retryAfter("ana@test.dev")).thenReturn(Duration.ZERO);
        when(users.findByEmail("ana@test.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@test.dev", "incorrecta")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(loginAttempts).recordFailure("ana@test.dev");
    }

    @Test
    void un_login_correcto_perdona_los_fallos_anteriores() {
        User ana = User.register(
                "ana@test.dev", "$hash$", "Ana", ZoneId.of("America/Lima"),
                new InviteCode("ABCD2345"), CLOCK.instant());

        when(loginAttempts.retryAfter("ana@test.dev")).thenReturn(Duration.ZERO);
        when(users.findByEmail("ana@test.dev")).thenReturn(Optional.of(ana));
        when(hasher.matches("correcta", "$hash$")).thenReturn(true);
        when(tokens.issue(ana)).thenReturn(
                new AccessTokenIssuer.IssuedToken("un-jwt", Duration.ofMinutes(30)));
        when(tokenHasher.hash(anyString())).thenReturn("hash-del-refresh");
        when(refreshTokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.login(new LoginCommand("ana@test.dev", "correcta"));

        verify(loginAttempts).clear("ana@test.dev");
    }
}
