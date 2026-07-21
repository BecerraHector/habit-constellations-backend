package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.user.User;

import java.time.Duration;

/** Emite las credenciales con las que el cliente autentica sus siguientes peticiones. */
public interface AccessTokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String accessToken, Duration expiresIn) {}
}
