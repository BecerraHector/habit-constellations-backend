package com.constellations.habits.application.user;

import com.constellations.habits.application.exception.EmailAlreadyUsedException;
import com.constellations.habits.application.exception.InvalidCredentialsException;
import com.constellations.habits.application.exception.InvalidRefreshTokenException;
import com.constellations.habits.application.port.out.AccessTokenIssuer;
import com.constellations.habits.application.port.out.PasswordHasher;
import com.constellations.habits.application.port.out.RefreshTokenRepository;
import com.constellations.habits.application.port.out.TokenHasher;
import com.constellations.habits.application.port.out.TransactionRunner;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.ValidationException;
import com.constellations.habits.domain.user.RefreshToken;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Casos de uso de registro y autenticacion. */
public class UserAccountService {

    /** Suficiente para frenar fuerza bruta sin castigar a quien usa frases largas. */
    private static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer tokens;
    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final InviteCodeAllocator inviteCodes;
    private final TransactionRunner transaction;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public UserAccountService(
            UserRepository users,
            PasswordHasher hasher,
            AccessTokenIssuer tokens,
            RefreshTokenRepository refreshTokens,
            TokenHasher tokenHasher,
            InviteCodeAllocator inviteCodes,
            TransactionRunner transaction,
            Duration refreshTokenTtl,
            Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.inviteCodes = inviteCodes;
        this.transaction = transaction;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public User register(RegisterUserCommand command) {
        String email = User.normalizeEmail(command.email());
        requireStrongEnough(command.password());
        ZoneId zone = parseZone(command.zoneId());

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        // El codigo de invitacion se asigna en el alta: cada cuenta nace lista para
        // recibir solicitudes sin un paso extra.
        User user = User.register(
                email,
                hasher.hash(command.password()),
                command.displayName(),
                zone,
                inviteCodes.allocate(),
                clock.instant());
        return users.save(user);
    }

    public AuthenticatedUser login(LoginCommand command) {
        String email = User.normalizeEmail(command.email());

        User user = users.findByEmail(email)
                .filter(candidate -> hasher.matches(command.password(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return authenticate(user);
    }

    /**
     * Cambia un token de refresco por uno de acceso nuevo, y lo rota: el usado se revoca
     * y se entrega otro. Asi una copia robada deja de servir en cuanto el dueno legitimo
     * renueva, en lugar de valer los treinta dias enteros.
     *
     * <p>Si se presenta un token <em>ya revocado</em> se revocan todas las sesiones del
     * usuario. Que aparezca dos veces significa que existe una copia, y no hay forma de
     * saber cual de las dos partes es la legitima: lo unico seguro es obligar a ambas a
     * volver a identificarse.
     */
    public AuthenticatedUser refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        Instant now = clock.instant();
        RefreshToken stored = refreshTokens.findByHash(tokenHasher.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked()) {
            // Fuera de transaccion a proposito: el cierre de sesiones tiene que quedar
            // escrito, y si compartiera transaccion con el fallo de abajo se desharia al
            // lanzar la excepcion, que es justo lo contrario de lo que se pretende.
            refreshTokens.revokeAllForUser(stored.userId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (stored.hasExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(stored.userId()).orElseThrow(InvalidRefreshTokenException::new);

        // Revocar el anterior y emitir el nuevo van juntos: si se cayera entre medias, el
        // usuario perderia la sesion sin haber hecho nada mal.
        return transaction.execute(() -> {
            refreshTokens.save(stored.revoke(now));
            return authenticate(user);
        });
    }

    /**
     * Cierra la sesion revocando el token de refresco. Es idempotente y nunca falla: si
     * el token ya no vale, decirlo solo confirmaria a un tercero que existio.
     */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByHash(tokenHasher.hash(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> refreshTokens.save(token.revoke(clock.instant())));
    }

    private AuthenticatedUser authenticate(User user) {
        var accessToken = tokens.issue(user);

        // El valor en claro solo existe aqui y en la respuesta: de la fila se guarda el
        // hash, de modo que leer la base de datos no basta para suplantar a nadie.
        String rawRefreshToken = RefreshToken.generateValue();
        refreshTokens.save(RefreshToken.issue(
                user.id(), tokenHasher.hash(rawRefreshToken), clock.instant(), refreshTokenTtl));

        return new AuthenticatedUser(
                user,
                accessToken.accessToken(),
                accessToken.expiresIn().toSeconds(),
                rawRefreshToken);
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
