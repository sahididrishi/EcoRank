package dev.ecorank.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.ecorank.backend.IntegrationTestBase;
import dev.ecorank.backend.repository.OrderRepository;
import dev.ecorank.backend.repository.PlayerRepository;
import dev.ecorank.backend.repository.ProductRepository;

class AdminIntegrationTest extends IntegrationTestBase {

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
    void login_withValidCredentials_returnsAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String body = "{\"username\":\"admin\",\"password\":\"admin\"}";

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/admin/auth/login",
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("accessToken")).isNotNull();
        assertThat(response.getBody().get("expiresIn")).isNotNull();
    }

    @Test
    void login_withInvalidCredentials_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String body = "{\"username\":\"admin\",\"password\":\"wrong\"}";

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/admin/auth/login",
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createProduct_withAuth_returns201() {
        HttpHeaders headers = adminAuthHeaders();

        String body = """
                {
                    "name": "Diamond Rank",
                    "slug": "diamond-rank",
                    "priceCents": 1999,
                    "description": "Premium diamond rank",
                    "rankGroup": "diamond",
                    "category": "ranks"
                }
                """;

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/products",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("slug")).isEqualTo("diamond-rank");
        assertThat(response.getBody().get("priceCents")).isEqualTo(1999);
    }

    @Test
    void createProduct_withoutAuth_returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String body = """
                {
                    "name": "Diamond Rank",
                    "slug": "diamond-rank",
                    "priceCents": 1999
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/products",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getProducts_withAuth_returnsProductList() {
        // Create a product first
        HttpHeaders headers = adminAuthHeaders();

        String createBody = """
                {
                    "name": "VIP Rank",
                    "slug": "vip-rank",
                    "priceCents": 999,
                    "rankGroup": "vip"
                }
                """;

        restTemplate.exchange(
                "/api/v1/admin/products",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );

        // Now list
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/admin/products",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void updateProduct_withAuth_updatesFields() {
        HttpHeaders headers = adminAuthHeaders();

        // Create
        String createBody = """
                {
                    "name": "VIP Rank",
                    "slug": "vip-rank-update",
                    "priceCents": 999
                }
                """;

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/admin/products",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );

        Number productId = (Number) createResponse.getBody().get("id");

        // Update
        String updateBody = """
                {
                    "name": "VIP Rank Pro",
                    "priceCents": 1499
                }
                """;

        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/v1/admin/products/" + productId.longValue(),
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                Map.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().get("name")).isEqualTo("VIP Rank Pro");
        assertThat(updateResponse.getBody().get("priceCents")).isEqualTo(1499);
    }

    @Test
    void getStats_withAuth_returnsDashboardStats() {
        HttpHeaders headers = adminAuthHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys("totalRevenueCents", "totalOrders", "totalPlayers", "pendingOrders");
    }
}
