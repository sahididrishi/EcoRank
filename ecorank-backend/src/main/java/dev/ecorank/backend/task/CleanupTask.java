package dev.ecorank.backend.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.ecorank.backend.repository.RefreshTokenRepository;
import dev.ecorank.backend.repository.WebhookEventRepository;

/**
 * Daily cleanup task that runs at 3:00 AM.
 * - Deletes processed webhook events older than 30 days.
 * - Deletes expired or revoked refresh tokens.
 */
@Component
public class CleanupTask {

    private static final Logger log = LoggerFactory.getLogger(CleanupTask.class);

    private final WebhookEventRepository webhookEventRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public CleanupTask(WebhookEventRepository webhookEventRepository,
                       RefreshTokenRepository refreshTokenRepository) {
        this.webhookEventRepository = webhookEventRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        // Delete processed webhook events older than 30 days
        Instant webhookCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deletedWebhooks = webhookEventRepository.deleteProcessedBefore(webhookCutoff);
        if (deletedWebhooks > 0) {
            log.info("Cleaned up {} processed webhook events older than 30 days", deletedWebhooks);
        }

        // Delete expired or revoked refresh tokens
        int deletedTokens = refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
        if (deletedTokens > 0) {
            log.info("Cleaned up {} expired/revoked refresh tokens", deletedTokens);
        }
    }
}
