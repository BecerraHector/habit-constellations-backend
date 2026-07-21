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

    /**
     * Solo se devuelve al propio usuario, nunca al mirar a un tercero: el codigo de
     * invitacion es una credencial para contactarle.
     */
    public record UserResponse(
            UUID id, String email, String displayName, String zoneId, String inviteCode) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id(),
                    user.email(),
                    user.displayName(),
                    user.zoneId().getId(),
                    user.inviteCode().formatted());
        }
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    /**
     * Darse de baja exige la contrasena. Es irreversible, y un token robado no deberia
     * bastar para borrarle la vida a nadie.
     */
    public record DeleteAccountRequest(@NotBlank String password) {}

    /**
     * El token de refresco se devuelve en el cuerpo y no en una cookie porque la API es
     * stateless y sirve tambien a clientes que no son navegadores. El cliente web deberia
     * guardarlo donde no lo alcance un script de terceros.
     */
    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            String refreshToken,
            UserResponse user) {

        public static TokenResponse from(AuthenticatedUser authenticated) {
            return new TokenResponse(
                    authenticated.accessToken(),
                    "Bearer",
                    authenticated.expiresInSeconds(),
                    authenticated.refreshToken(),
                    UserResponse.from(authenticated.user()));
        }
    }
}
