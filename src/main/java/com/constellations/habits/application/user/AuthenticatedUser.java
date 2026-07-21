package com.constellations.habits.application.user;

import com.constellations.habits.domain.user.User;

/**
 * @param refreshToken valor en claro; es la unica vez que existe fuera del cliente,
 *                     porque de el solo se guarda el hash
 */
public record AuthenticatedUser(
        User user, String accessToken, long expiresInSeconds, String refreshToken) {}
