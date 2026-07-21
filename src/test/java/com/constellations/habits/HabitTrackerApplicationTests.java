package com.constellations.habits;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba que el contexto entero levanta contra un Postgres real: incluye que Flyway
 * aplique las migraciones y que Hibernate valide el esquema resultante.
 */
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
class HabitTrackerApplicationTests {

    @Test
    void contextLoads() {
    }
}
