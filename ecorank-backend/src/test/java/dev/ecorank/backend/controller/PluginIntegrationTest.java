package dev.ecorank.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.ecorank.backend.IntegrationTestBase;
import dev.ecorank.backend.entity.Order;
import dev.ecorank.backend.entity.OrderStatus;
import dev.ecorank.backend.entity.Player;
import dev.ecorank.backend.entity.Product;
import dev.ecorank.backend.repository.OrderRepository;
import dev.ecorank.backend.repository.PlayerRepository;
import dev.ecorank.backend.repository.ProductRepository;

class PluginIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Player testPlayer;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        playerRepository.deleteAll();
        productRepository.deleteAll();

        testPlayer = new Player(UUID.randomUUID(), "TestPlayer");
        testPlayer = playerRepository.save(testPlayer);

        testProduct = new Product();
        testProduct.setSlug("vip-rank");
        testProduct.setName("VIP Rank");
        testProduct.setPriceCents(999);
        testProduct.setRankGroup("vip");
        testProduct.setActive(true);
        testProduct.setSortOrder(0);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void getPendingOrders_withApiKey_returnsOrders() {
        // Create a QUEUED order
        Order order = new Order();
        order.setPlayer(testPlayer);
        order.setProduct(testProduct);
        order.setIdempotencyKey(UUID.randomUUID());
        order.setAmountCents(999);
        order.setStatus(OrderStatus.QUEUED);
        order.setPaymentProvider("stripe");
        orderRepository.save(order);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/plugin/orders/pending",
                HttpMethod.GET,
                new HttpEntity<>(apiKeyHeaders()),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getPendingOrders_withoutApiKey_returns401() {
        HttpHeaders noAuthHeaders = new HttpHeaders();
        noAuthHeaders.set("Content-Type", "application/json");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/plugin/orders/pending",
                HttpMethod.GET,
                new HttpEntity<>(noAuthHeaders),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void confirmFulfillment_marksOrderFulfilled() {
        // Create a QUEUED order
        Order order = new Order();
        order.setPlayer(testPlayer);
        order.setProduct(testProduct);
        order.setIdempotencyKey(UUID.randomUUID());
        order.setAmountCents(999);
        order.setStatus(OrderStatus.QUEUED);
        order.setPaymentProvider("stripe");
        order = orderRepository.save(order);

        String body = "{\"serverId\":\"lobby-1\"}";

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/plugin/orders/" + order.getId() + "/confirm",
                HttpMethod.POST,
                new HttpEntity<>(body, apiKeyHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("FULFILLED");

        // Verify in DB
        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(updated.getServerId()).isEqualTo("lobby-1");
        assertThat(updated.getFulfilledAt()).isNotNull();
    }

    @Test
    void playerJoin_createsOrUpdatesPlayer() {
        UUID newUuid = UUID.randomUUID();
        String body = String.format("{\"uuid\":\"%s\",\"username\":\"NewPlayer\"}", newUuid);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/plugin/players/join",
                HttpMethod.POST,
                new HttpEntity<>(body, apiKeyHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify player was created
        assertThat(playerRepository.findByMinecraftUuid(newUuid)).isPresent();
    }
}
