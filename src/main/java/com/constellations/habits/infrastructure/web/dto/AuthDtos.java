package com.constellations.habits.infrastructure.web.dto;

import com.constellations.habits.application.user.AuthenticatedUser;
import com.constellations.habits.domain.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Contratos HTTP de autenticacion.
 *
 * <p>Se validan aqui, en el borde, para devolver 400 con detalle de campo antes de
 * molestar al dominio; el dominio revalida igualmente porque no confia en nadie.
 */
public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 10, max = 128) String password,
            @NotBlank @Size(max = 60) String displayName,
            @Size(max = 64) String zoneId) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record UserResponse(UUID id, String email, String displayName, String zoneId) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id(), user.email(), user.displayName(), user.zoneId().getId());
        }
    }

    public record TokenResponse(
            String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {

        public static TokenResponse from(AuthenticatedUser authenticated) {
            return new TokenResponse(
                    authenticated.accessToken(),
                    "Bearer",
                    authenticated.expiresInSeconds(),
                    UserResponse.from(authenticated.user()));
        }
    }
}
