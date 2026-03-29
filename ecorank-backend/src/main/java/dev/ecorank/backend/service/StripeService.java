package dev.ecorank.backend.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import dev.ecorank.backend.config.EcoRankProperties;
import dev.ecorank.backend.dto.request.CreateCheckoutRequest;
import dev.ecorank.backend.entity.Order;
import dev.ecorank.backend.entity.Player;
import dev.ecorank.backend.entity.Product;
import dev.ecorank.backend.exception.PaymentProcessingException;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final EcoRankProperties properties;
    private final ProductService productService;
    private final PlayerService playerService;
    private final OrderService orderService;
    private final WebhookService webhookService;

    public StripeService(EcoRankProperties properties,
                         ProductService productService,
                         PlayerService playerService,
                         OrderService orderService,
                         WebhookService webhookService) {
        this.properties = properties;
        this.productService = productService;
        this.playerService = playerService;
        this.orderService = orderService;
        this.webhookService = webhookService;
        Stripe.apiKey = properties.getStripe().getSecretKey();
    }

    public String createCheckoutSession(CreateCheckoutRequest request) {
        Product product = productService.getProductBySlug(request.productSlug());

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://your-domain.com/store/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("https://your-domain.com/store/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount((long) product.getPriceCents())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(product.getName())
                                                                    .setDescription(product.getDescription())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("product_slug", product.getSlug())
                    .putMetadata("player_uuid", request.playerUuid().toString())
                    .putMetadata("player_name", request.playerName())
                    .putMetadata("idempotency_key", request.idempotencyKey().toString())
                    .setClientReferenceId(request.idempotencyKey().toString())
                    .build();

            Session session = Session.create(params);
            log.info("Stripe checkout session created: {} for product: {}", session.getId(), product.getSlug());
            return session.getUrl();

        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create Stripe checkout session", e);
        }
    }

    /**
     * Handle a raw Stripe webhook event.
     * CRITICAL: payload is the raw request body string — not parsed by Jackson.
     */
    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    properties.getStripe().getWebhookSecret()
            );
        } catch (SignatureVerificationException e) {
            throw new PaymentProcessingException("Invalid Stripe webhook signature", e);
        }

        String eventId = event.getId();
        String eventType = event.getType();

        // Idempotency check
        if (webhookService.isAlreadyProcessed(eventId)) {
            log.info("Stripe webhook {} already processed, returning early", eventId);
            return;
        }

        // Save the raw event — null means a concurrent thread already inserted it
        if (webhookService.saveRawEvent(eventId, "stripe", eventType, payload) == null) {
            log.info("Stripe webhook {} was a concurrent duplicate, skipping", eventId);
            return;
        }

        try {
            switch (eventType) {
                case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
                case "charge.refunded" -> handleChargeRefunded(event);
                default -> log.info("Unhandled Stripe event type: {}", eventType);
            }

            webhookService.markProcessed(eventId);
        } catch (Exception e) {
            log.error("Error processing Stripe webhook {} (type: {}): {}", eventId, eventType, e.getMessage(), e);
            // Don't rethrow — we always return 200 to Stripe. The retry task will pick this up.
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentProcessingException("Failed to deserialize checkout session"));

        Map<String, String> metadata = session.getMetadata();
        String productSlug = metadata.get("product_slug");
        String playerUuidStr = metadata.get("player_uuid");
        String playerName = metadata.get("player_name");
        String idempotencyKeyStr = metadata.get("idempotency_key");

        UUID playerUuid = UUID.fromString(playerUuidStr);
        UUID idempotencyKey = UUID.fromString(idempotencyKeyStr);

        Player player = playerService.getOrCreatePlayer(playerUuid, playerName);
        Product product = productService.getProductBySlug(productSlug);

        Order order = orderService.createOrder(player, product, idempotencyKey, "stripe");
        orderService.markPaid(order.getId(), "stripe", session.getPaymentIntent());

        log.info("Checkout session completed: order {} for player {} product {}",
                order.getId(), playerName, productSlug);
    }

    private void handleChargeRefunded(Event event) {
        Charge charge = (Charge) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentProcessingException("Failed to deserialize charge.refunded event"));

        String paymentId = charge.getPaymentIntent();
        try {
            Order order = orderService.findByProviderPaymentId(paymentId);
            orderService.markRefunded(order.getId());
            log.info("Charge refunded for payment {}, order {}", paymentId, order.getId());
        } catch (Exception e) {
            log.warn("Could not find order for refunded payment {}: {}", paymentId, e.getMessage());
        }
    }
}
