package dev.ecorank.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stub PayPal service implementation.
 * PayPal integration is planned but not yet implemented.
 */
@Service
public class PayPalService {

    private static final Logger log = LoggerFactory.getLogger(PayPalService.class);

    public void handleWebhookEvent(String payload) {
        log.warn("PayPal webhook received but PayPal integration is not yet implemented. Payload length: {}",
                payload != null ? payload.length() : 0);
    }
}
