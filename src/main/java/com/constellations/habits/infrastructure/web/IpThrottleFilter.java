package com.constellations.habits.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ventana fija de peticiones por IP y minuto sobre los endpoints donde probar a ciegas
 * tiene premio: contrasenas en el login y codigos de invitacion en las solicitudes.
 *
 * <p>Corre antes que Spring Security a proposito: una IP bloqueada no debe costar ni un
 * calculo de BCrypt. Detras de un proxy inverso hay que activar
 * {@code server.forward-headers-strategy} para que {@code getRemoteAddr} vea la IP real.
 */
@Component
// La cadena de Spring Security se registra en -100; esto corre justo antes.
@Order(-110)
public class IpThrottleFilter extends OncePerRequestFilter {

    private static final Set<String> GUARDED_PATHS =
            Set.of("/api/v1/auth/login", "/api/v1/friend-requests");

    private record Window(long epochMinute, AtomicInteger count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ThrottleProperties properties;
    private final Clock clock;

    public IpThrottleFilter(ThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod())
                && GUARDED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long minute = clock.instant().getEpochSecond() / 60;
        Window window = windows.compute(request.getRemoteAddr(), (ip, current) ->
                current == null || current.epochMinute() != minute
                        ? new Window(minute, new AtomicInteger())
                        : current);

        if (window.count().incrementAndGet() > properties.ipLimit()) {
            reject(response);
            return;
        }

        // La ventana anterior de cada IP queda obsoleta; se poda para no crecer sin fin.
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().epochMinute() != minute);
        }

        chain.doFilter(request, response);
    }

    /** ProblemDetail escrito a mano: este filtro corre fuera del mundo MVC. */
    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":429,"title":"Too Many Requests",\
                "detail":"Demasiadas peticiones desde esta direccion. Espera un minuto"}""");
    }
}
