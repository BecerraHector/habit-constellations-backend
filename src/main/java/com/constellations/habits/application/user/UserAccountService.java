package com.constellations.habits.application.user;

import com.constellations.habits.application.exception.EmailAlreadyUsedException;
import com.constellations.habits.application.exception.InvalidCredentialsException;
import com.constellations.habits.application.port.out.AccessTokenIssuer;
import com.constellations.habits.application.port.out.PasswordHasher;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.ValidationException;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;

/** Casos de uso de registro y autenticacion. */
public class UserAccountService {

    /** Suficiente para frenar fuerza bruta sin castigar a quien usa frases largas. */
    private static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer tokens;
    private final Clock clock;

    public UserAccountService(
            UserRepository users, PasswordHasher hasher, AccessTokenIssuer tokens, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.clock = clock;
    }

    public User register(RegisterUserCommand command) {
        String email = User.normalizeEmail(command.email());
        requireStrongEnough(command.password());
        ZoneId zone = parseZone(command.zoneId());

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = User.register(
                email, hasher.hash(command.password()), command.displayName(), zone, clock.instant());
        return users.save(user);
    }

    public AuthenticatedUser login(LoginCommand command) {
        String email = User.normalizeEmail(command.email());

        User user = users.findByEmail(email)
                .filter(candidate -> hasher.matches(command.password(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        var token = tokens.issue(user);
        return new AuthenticatedUser(user, token.accessToken(), token.expiresIn().toSeconds());
    }

    private static void requireStrongEnough(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "La contrasena debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }

    private static ZoneId parseZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (DateTimeException e) {
            throw new ValidationException("Zona horaria desconocida: " + zoneId);
        }
    }
}
