package com.constellations.habits.infrastructure.web;

import com.constellations.habits.application.user.LoginCommand;
import com.constellations.habits.application.user.RegisterUserCommand;
import com.constellations.habits.application.user.UserAccountService;
import com.constellations.habits.infrastructure.security.AuthenticatedUserId;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.LoginRequest;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.RegisterRequest;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.TokenResponse;
import com.constellations.habits.infrastructure.web.dto.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /** Quien soy: util para que el cliente compruebe que su token sigue vivo. */
    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AuthenticatedUserId principal) {
        return users.findById(principal.value())
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(principal.value()));
    }
}
