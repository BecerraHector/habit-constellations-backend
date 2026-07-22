package com.constellations.habits.infrastructure.web;

import com.constellations.habits.application.user.LoginCommand;
import com.constellations.habits.application.user.RegisterUserCommand;
import com.constellations.habits.application.user.UserAccountService;
import com.constellations.habits.infrastructure.security.AuthenticatedUserId;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.LoginRequest;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.DeleteAccountRequest;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.RefreshRequest;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.RegisterRequest;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.TokenResponse;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.application.port.out.UserRepository;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final UserAccountService accounts;
    private final UserRepository users;

    AuthController(UserAccountService accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = accounts.register(new RegisterUserCommand(
                request.email(), request.password(), request.displayName(), request.zoneId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(
                accounts.login(new LoginCommand(request.email(), request.password())));
    }

    /**
     * Renueva el token de acceso sin volver a pedir la contrasena. Rota el de refresco:
     * el presentado queda revocado y se entrega uno nuevo en la respuesta.
     */
    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(accounts.refresh(request.refreshToken()));
    }

    /** Cierra la sesion. Idempotente: repetirlo no es un error ni confirma nada. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) {
        accounts.logout(request.refreshToken());
    }

    /**
     * Cierra todas las sesiones del usuario, la actual incluida: revoca cada token de
     * refresco vivo. Pensado para el "cerrar sesion en todos los dispositivos" tras
     * perder un movil o sospechar de un acceso ajeno.
     */
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logoutEverywhere(@AuthenticationPrincipal AuthenticatedUserId principal) {
        accounts.logoutEverywhere(principal.value());
    }

    /**
     * Da de baja la cuenta. Los habitos privados desaparecen; las estrellas que este
     * usuario aporto a una galaxia se conservan como recuento anonimo, porque el mapa de
     * los demas no deberia reescribirse porque alguien se vaya.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @Valid @RequestBody DeleteAccountRequest request) {
        accounts.deleteAccount(principal.value(), request.password());
    }

    /** Quien soy: util para que el cliente compruebe que su token sigue vivo. */
    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AuthenticatedUserId principal) {
        return users.findById(principal.value())
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(principal.value()));
    }
}
