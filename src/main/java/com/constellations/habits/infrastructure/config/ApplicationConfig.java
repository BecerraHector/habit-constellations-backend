package com.constellations.habits.infrastructure.config;

import com.constellations.habits.application.habit.HabitService;
import com.constellations.habits.application.port.out.AccessTokenIssuer;
import com.constellations.habits.application.port.out.HabitLogRepository;
import com.constellations.habits.application.port.out.HabitRepository;
import com.constellations.habits.application.port.out.PasswordHasher;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.application.user.UserAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Ensambla los casos de uso.
 *
 * <p>Los servicios de aplicacion son POJOs sin anotaciones de Spring, para que la capa
 * pueda testearse (y en teoria reutilizarse) sin arrancar el contenedor. El precio es
 * declararlos aqui a mano, que es exactamente donde debe vivir esa decision.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Reloj inyectable en UTC. Los tests lo sustituyen por uno fijo en lugar de
     * depender de la hora real de la maquina.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UserAccountService userAccountService(
            UserRepository users, PasswordHasher hasher, AccessTokenIssuer tokens, Clock clock) {
        return new UserAccountService(users, hasher, tokens, clock);
    }

    @Bean
    HabitService habitService(
            HabitRepository habits, HabitLogRepository logs, UserRepository users, Clock clock) {
        return new HabitService(habits, logs, users, clock);
    }
}
