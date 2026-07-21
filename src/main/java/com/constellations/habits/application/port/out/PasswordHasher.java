package com.constellations.habits.application.port.out;

/** Aisla los casos de uso del algoritmo de hashing concreto (hoy BCrypt). */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String storedHash);
}
