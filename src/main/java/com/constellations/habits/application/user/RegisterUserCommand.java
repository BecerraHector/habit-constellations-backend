package com.constellations.habits.application.user;

/** @param zoneId zona IANA (ej. "America/Lima"); si es null se asume UTC */
public record RegisterUserCommand(String email, String password, String displayName, String zoneId) {}
