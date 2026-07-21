package com.constellations.habits.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "habits.security.jwt")
public record JwtProperties(
        String secret, Duration accessTokenTtl, Duration refreshTokenTtl, String issuer) {

    /** HS256 exige una clave de al menos 256 bits para que la firma valga algo. */
    public static final int MIN_SECRET_BYTES = 32;
}
