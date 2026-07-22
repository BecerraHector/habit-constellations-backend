package com.constellations.habits.application.user;

import com.constellations.habits.application.exception.EmailAlreadyUsedException;
import com.constellations.habits.application.exception.InvalidCredentialsException;
import com.constellations.habits.application.exception.InvalidRefreshTokenException;
import com.constellations.habits.application.galaxy.GalaxyService;
import com.constellations.habits.application.port.out.AccessTokenIssuer;
import com.constellations.habits.application.port.out.FriendshipRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.PasswordHasher;
import com.constellations.habits.application.port.out.RefreshTokenRepository;
import com.constellations.habits.application.port.out.TokenHasher;
import com.constellations.habits.application.port.out.TransactionRunner;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.domain.ValidationException;
import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.user.RefreshToken;
import com.constellations.habits.domain.user.User;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Casos de uso de registro y autenticacion. */
public class UserAccountService {

    /** Suficiente para frenar fuerza bruta sin castigar a quien usa frases largas. */
    private static final int MIN_PASSWORD_LENGTH = 10;

    /** Lo que queda de un habito cuyo dueno se dio de baja pero cuyas estrellas siguen. */
    private static final String ANONYMIZED_HABIT_NAME = "Habito de una cuenta eliminada";

    private final UserRepository users;
    private final HabitRepository habits;
    private final FriendshipRepository friendships;
    private final GalaxyService galaxies;
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
            HabitRepository habits,
            FriendshipRepository friendships,
            GalaxyService galaxies,
            PasswordHasher hasher,
            AccessTokenIssuer tokens,
            RefreshTokenRepository refreshTokens,
            TokenHasher tokenHasher,
            InviteCodeAllocator inviteCodes,
            TransactionRunner transaction,
            Duration refreshTokenTtl,
            Clock clock) {
        this.users = users;
        this.habits = habits;
        this.friendships = friendships;
        this.galaxies = galaxies;
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

    /**
     * Cierra todas las sesiones del usuario revocando sus tokens de refresco. Los tokens
     * de acceso ya emitidos siguen valiendo hasta caducar (minutos): es el precio de que
     * los JWT no se comprueben contra la base en cada peticion.
     */
    public void logoutEverywhere(UUID userId) {
        refreshTokens.revokeAllForUser(userId, clock.instant());
    }

    /**
     * Da de baja la cuenta. Pide la contrasena porque es irreversible y un token robado
     * no deberia bastar para borrarle la vida a nadie.
     *
     * <p>Lo que ocurre, y por que:
     * <ul>
     *   <li>Sale de todas sus galaxias con fecha de hoy. Desde este momento el grupo no
     *       la espera: ni cuenta en el denominador ni su ausencia oscurece nada.</li>
     *   <li>Sus habitos privados se borran de verdad, con sus registros. Nadie mas los
     *       vio nunca.</li>
     *   <li>Los habitos que alimentaban una galaxia conservan sus registros pero pierden
     *       el nombre. "Terapia los martes" dice mucho de alguien; una fila
     *       {@code (habito, fecha)} sin nombre es un recuento anonimo, y el brillo de los
     *       dias ya vividos por otras personas depende de el.</li>
     *   <li>La fila del usuario queda como lapida anonima, sin email, sin contrasena
     *       utilizable y sin el codigo de invitacion.</li>
     * </ul>
     */
    public void deleteAccount(UUID userId, String password) {
        User user = users.findById(userId)
                .filter(candidate -> !candidate.isDeleted())
                .filter(candidate -> hasher.matches(password, candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        LocalDate today = user.today(clock.instant());
        Set<UUID> linkedToGalaxies = galaxies.releaseAllMembershipsOf(userId, today);

        transaction.run(() -> {
            List<Habit> own = habits.findAllByOwner(userId);
            Instant now = clock.instant();

            List<UUID> disposable = own.stream()
                    .map(Habit::id)
                    .filter(habitId -> !linkedToGalaxies.contains(habitId))
                    .toList();
            habits.deleteAll(disposable);

            own.stream()
                    .filter(habit -> linkedToGalaxies.contains(habit.id()))
                    .forEach(habit -> habits.save(
                            habit.anonymize(ANONYMIZED_HABIT_NAME, now)));

            friendships.deleteAllInvolving(userId);
            refreshTokens.deleteAllForUser(userId);
            users.save(user.anonymize(inviteCodes.allocate(), now));
        });
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
