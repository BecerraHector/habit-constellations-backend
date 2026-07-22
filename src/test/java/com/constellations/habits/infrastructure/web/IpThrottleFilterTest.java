package com.constellations.habits.infrastructure.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class IpThrottleFilterTest {

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
    private final IpThrottleFilter filter =
            new IpThrottleFilter(new ThrottleProperties(3), clock);

    @Test
    void dentro_del_tope_la_peticion_pasa() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            assertThat(hitLogin("10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void por_encima_del_tope_responde_429_sin_tocar_la_cadena() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            hitLogin("10.0.0.1");
        }

        MockHttpServletResponse blocked = hitLogin("10.0.0.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentType()).contains("problem+json");
    }

    @Test
    void cada_ip_tiene_su_propia_ventana() throws ServletException, IOException {
        for (int i = 0; i < 4; i++) {
            hitLogin("10.0.0.1");
        }

        assertThat(hitLogin("10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void al_minuto_siguiente_la_ventana_se_reinicia() throws ServletException, IOException {
        for (int i = 0; i < 4; i++) {
            hitLogin("10.0.0.1");
        }
        clock.advance(Duration.ofMinutes(1));

        assertThat(hitLogin("10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void las_rutas_no_vigiladas_no_se_cuentan() throws ServletException, IOException {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/habits");
            request.setRemoteAddr("10.0.0.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse hitLogin(String ip) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
