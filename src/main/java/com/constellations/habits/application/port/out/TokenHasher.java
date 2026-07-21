package com.constellations.habits.application.port.out;

/**
 * Resume un token opaco para poder guardarlo sin almacenarlo en claro.
 *
 * <p>Es un puerto distinto de {@link PasswordHasher} a proposito. Una contrasena la
 * elige una persona y tiene poca entropia, asi que necesita un hash lento y con sal que
 * encarezca la fuerza bruta. Un token de 256 bits aleatorios no se puede adivinar por
 * fuerza bruta, de modo que basta un digest rapido, y ademas debe ser
 * <em>determinista</em>: la busqueda se hace por hash.
 */
public interface TokenHasher {

    String hash(String rawToken);
}
