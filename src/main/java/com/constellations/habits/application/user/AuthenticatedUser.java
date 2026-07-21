package com.constellations.habits.application.user;

import com.constellations.habits.domain.user.User;

public record AuthenticatedUser(User user, String accessToken, long expiresInSeconds) {}
