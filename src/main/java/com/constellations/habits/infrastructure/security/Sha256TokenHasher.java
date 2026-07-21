package com.constellations.habits.infrastructure.security;

import com.constellations.habits.application.port.out.TokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 sin sal, a diferencia del BCrypt de las contrasenas.
 *
 * <p>Es deliberado y no un descuido. Una contrasena la elige una persona y tiene poca
 * entropia: necesita un hash lento y con sal para que probar el diccionario salga caro.
 * Un token de 256 bits aleatorios no esta en ningun diccionario, asi que lo unico que
 * aportaria BCrypt es latencia. Y hace falta que sea determinista, porque la sesion se
 * localiza buscando precisamente por este hash.
 */
@Component
class Sha256TokenHasher implements TokenHasher {

    @Override
    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si falta, algo mucho peor pasa.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
