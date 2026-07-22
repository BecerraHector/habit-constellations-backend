package com.constellations.habits.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptLimiterTest {

    /** Reloj que solo avanza cuando el test lo empuja. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-07-22T12:00:00Z");

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final InMemoryLoginAttemptLimiter limiter = new InMemoryLoginAttemptLimiter(clock);

    @Test
    void los_primeros_fallos_no_castigan() {
        for (int i = 0; i < InMemoryLoginAttemptLimiter.FREE_FAILURES; i++) {
            limiter.recordFailure("ana@test.dev");
        }

        assertThat(limiter.retryAfter("ana@test.dev")).isZero();
    }

    @Test
    void el_cuarto_fallo_impone_la_espera_base() {
        failTimes("ana@test.dev", 4);

        assertThat(limiter.retryAfter("ana@test.dev"))
                .isEqualTo(InMemoryLoginAttemptLimiter.BASE_DELAY);
    }

    @Test
    void cada_fallo_extra_duplica_la_espera() {
        failTimes("ana@test.dev", 6);

        assertThat(limiter.retryAfter("ana@test.dev"))
                .isEqualTo(InMemoryLoginAttemptLimiter.BASE_DELAY.multipliedBy(4));
    }

    @Test
    void la_espera_no_crece_por_encima_del_tope() {
        failTimes("ana@test.dev", 40);

        assertThat(limiter.retryAfter("ana@test.dev"))
                .isEqualTo(InMemoryLoginAttemptLimiter.MAX_DELAY);
    }

    @Test
    void la_espera_se_consume_con_el_tiempo() {
        failTimes("ana@test.dev", 4);
        clock.advance(InMemoryLoginAttemptLimiter.BASE_DELAY.minusSeconds(10));

        assertThat(limiter.retryAfter("ana@test.dev")).isEqualTo(Duration.ofSeconds(10));

        clock.advance(Duration.ofSeconds(10));
        assertThat(limiter.retryAfter("ana@test.dev")).isZero();
    }

    @Test
    void el_exito_perdona_los_fallos_anteriores() {
        failTimes("ana@test.dev", 4);
        limiter.clear("ana@test.dev");

        assertThat(limiter.retryAfter("ana@test.dev")).isZero();
    }

    @Test
    void una_cuenta_tranquila_se_olvida_del_todo() {
        failTimes("ana@test.dev", 10);
        clock.advance(InMemoryLoginAttemptLimiter.FORGET_AFTER);

        assertThat(limiter.retryAfter("ana@test.dev")).isZero();
    }

    @Test
    void cada_cuenta_lleva_su_propia_cuenta() {
        failTimes("ana@test.dev", 10);

        assertThat(limiter.retryAfter("otro@test.dev")).isZero();
    }

    private void failTimes(String key, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(key);
        }
    }
}
