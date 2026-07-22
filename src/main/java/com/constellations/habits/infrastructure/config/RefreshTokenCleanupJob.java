package com.constellations.habits.infrastructure.config;

import com.constellations.habits.application.port.out.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Barrido diario de la tabla {@code refresh_tokens}: sin el, cada login deja una fila
 * para siempre. Solo caen las ya caducadas — las revocadas y aun vigentes se quedan,
 * porque son las que permiten detectar la reutilizacion de un token robado.
 */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    RefreshTokenCleanupJob(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    /** De madrugada UTC, cuando menos molesta. La hora exacta no importa. */
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeExpired() {
        int deleted = refreshTokens.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Limpieza de sesiones: {} tokens de refresco caducados eliminados", deleted);
        }
    }
}
