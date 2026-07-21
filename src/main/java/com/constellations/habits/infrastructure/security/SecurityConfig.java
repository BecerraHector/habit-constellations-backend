package com.constellations.habits.infrastructure.security;

import com.constellations.habits.application.port.out.PasswordHasher;
import com.constellations.habits.infrastructure.web.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtFilter, CorsProperties cors)
            throws Exception {
        return http
                // La API es stateless y se autentica por token, no por cookie de sesion:
                // sin cookie de sesion no hay vector CSRF que proteger.
                .csrf(csrf -> csrf.disable())
                .cors(configurer -> {
                    if (cors.isEnabled()) {
                        configurer.configurationSource(corsSource(cors));
                    } else {
                        configurer.disable();
                    }
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Se enumeran en vez de abrir /auth/** entero: bajo ese prefijo
                        // tambien viven /me y la baja de cuenta, que exigen sesion. Un
                        // comodin aqui los dejaba pasar sin token y el principal llegaba
                        // nulo, con lo que un 401 limpio se convertia en un 500.
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // La documentacion describe el contrato, no expone datos.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                // 401 seco en vez del formulario de login por defecto.
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * El navegador exige que el preflight lo responda el servidor antes de dejar salir
     * la peticion real. Sin esto, un frontend en otro puerto no llega ni a intentarlo.
     *
     * <p>{@code allowCredentials} queda en false: el token viaja en la cabecera
     * {@code Authorization}, no en una cookie, asi que no hay credenciales de navegador
     * que compartir y activarlo solo ampliaria la superficie.
     */
    private static CorsConfigurationSource corsSource(CorsProperties properties) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(Duration.ofHours(1));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder encoder) {
        return new PasswordHasher() {
            @Override
            public String hash(String rawPassword) {
                return encoder.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String storedHash) {
                return encoder.matches(rawPassword, storedHash);
            }
        };
    }
}
