package dev.ecorank.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ecorank.backend.exception.PaymentProcessingException;
import dev.ecorank.backend.service.PayPalService;
import dev.ecorank.backend.service.StripeService;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final StripeService stripeService;
    private final PayPalService payPalService;

    public WebhookController(StripeService stripeService, PayPalService payPalService) {
        this.stripeService = stripeService;
        this.payPalService = payPalService;
    }

    /**
     * Stripe webhook endpoint.
     * CRITICAL: Accept raw String body, not a DTO.
     * If Jackson parses and re-serializes the body, the signature will never match.
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.debug("Received Stripe webhook, signature header present: {}", sigHeader != null);

        try {
            stripeService.handleWebhookEvent(payload, sigHeader);
        } catch (PaymentProcessingException e) {
            if (e.getMessage() != null && e.getMessage().contains("Invalid Stripe webhook signature")) {
                log.warn("Invalid Stripe webhook signature rejected");
                return ResponseEntity.badRequest().body("invalid signature");
            }
            log.error("Error processing Stripe webhook: {}", e.getMessage());
            // Return 200 to prevent Stripe retries for events we've already saved
        } catch (Exception e) {
            log.error("Error handling Stripe webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok("ok");
    }

    @PostMapping("/paypal")
    public ResponseEntity<String> handlePayPalWebhook(@RequestBody String payload) {
        log.debug("Received PayPal webhook");

        try {
            payPalService.handleWebhookEvent(payload);
        } catch (Exception e) {
            log.error("Error handling PayPal webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok("ok");
    }
}
