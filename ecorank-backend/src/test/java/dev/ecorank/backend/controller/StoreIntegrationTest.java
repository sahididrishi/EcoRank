package dev.ecorank.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.ecorank.backend.IntegrationTestBase;
import dev.ecorank.backend.entity.Product;
import dev.ecorank.backend.repository.OrderRepository;
import dev.ecorank.backend.repository.PlayerRepository;
import dev.ecorank.backend.repository.ProductRepository;

class StoreIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        playerRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void getActiveProducts_returnsOnlyActiveProducts() {
        // Create active product
        Product active = new Product();
        active.setSlug("active-rank");
        active.setName("Active Rank");
        active.setPriceCents(999);
        active.setActive(true);
        active.setSortOrder(0);
        productRepository.save(active);

        // Create inactive product
        Product inactive = new Product();
        inactive.setSlug("inactive-rank");
        inactive.setName("Inactive Rank");
        inactive.setPriceCents(499);
        inactive.setActive(false);
        inactive.setSortOrder(1);
        productRepository.save(inactive);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/store/products",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getActiveProducts_emptyStore_returnsEmptyList() {
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/store/products",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void checkout_withInvalidRequest_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Missing required fields
        String body = "{}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/store/checkout",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkout_withNonExistentProduct_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String body = """
                {
                    "productSlug": "non-existent-product",
                    "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
                    "playerName": "TestPlayer",
                    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440001"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/store/checkout",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        // Should be 404 since product doesn't exist
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
