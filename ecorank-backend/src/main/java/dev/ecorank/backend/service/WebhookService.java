package dev.ecorank.backend.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ecorank.backend.entity.WebhookEvent;
import dev.ecorank.backend.repository.WebhookEventRepository;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository webhookEventRepository;

    public WebhookService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    /**
     * Check if a webhook event has already been processed.
     * Uses existsByEventId + save with try/catch for DataIntegrityViolation
     * to implement the INSERT ON CONFLICT pattern for idempotency.
     *
     * @return true if the event was already saved (duplicate), false if this is new
     */
    @Transactional
    public boolean isAlreadyProcessed(String eventId) {
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("Webhook event {} already exists, skipping", eventId);
            return true;
        }
        return false;
    }

    @Transactional
    public WebhookEvent saveRawEvent(String eventId, String provider, String eventType, String payload) {
        try {
            WebhookEvent event = new WebhookEvent(eventId, provider, eventType, payload);
            return webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread inserted first — that's fine, this is a duplicate
            log.info("Webhook event {} was concurrently inserted, treating as duplicate", eventId);
            return null;
        }
    }

    @Transactional
    public void markProcessed(String eventId) {
        webhookEventRepository.findByEventId(eventId).ifPresent(event -> {
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
        });
    }
}
