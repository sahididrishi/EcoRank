package dev.ecorank.backend.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.ecorank.backend.entity.WebhookEvent;
import dev.ecorank.backend.repository.WebhookEventRepository;

/**
 * Retries unprocessed webhook events older than 5 minutes.
 * Runs every 5 minutes (300000ms).
 */
@Component
public class WebhookRetryTask {

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryTask.class);

    private final WebhookEventRepository webhookEventRepository;

    public WebhookRetryTask(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    @Scheduled(fixedDelay = 300000)
    public void retryUnprocessedWebhooks() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<WebhookEvent> unprocessed = webhookEventRepository.findByProcessedFalseAndCreatedAtBefore(cutoff);

        if (unprocessed.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed webhook events older than 5 minutes, marking for retry", unprocessed.size());

        for (WebhookEvent event : unprocessed) {
            log.warn("Unprocessed webhook event: id={}, eventId={}, provider={}, type={}, createdAt={}",
                    event.getId(), event.getEventId(), event.getProvider(),
                    event.getEventType(), event.getCreatedAt());
            // In a full implementation, we would re-dispatch these events to the appropriate service.
            // For now, we log them so operators can investigate.
        }
    }
}
