package dev.ecorank.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class IntegrationTestBase {

    protected static final String TEST_API_KEY = "test-plugin-api-key";
    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("ecorank_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("ecorank.plugin.api-key", () -> TEST_API_KEY);
        registry.add("ecorank.jwt.secret", () -> "test-jwt-secret-key-that-is-at-least-32-characters-long");
        registry.add("ecorank.stripe.secret-key", () -> "sk_test_placeholder");
        registry.add("ecorank.stripe.webhook-secret", () -> "whsec_test_placeholder");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Returns HTTP headers with the plugin API key set.
     */
    protected HttpHeaders apiKeyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", TEST_API_KEY);
        headers.set("X-Server-Id", "test-server-1");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Login as admin and return HTTP headers with the Bearer JWT token.
     */
    protected HttpHeaders adminAuthHeaders() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.set("Content-Type", "application/json");

        String loginBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", ADMIN_USERNAME, ADMIN_PASSWORD);

        var loginResponse = restTemplate.postForEntity(
                "/api/v1/admin/auth/login",
                new org.springframework.http.HttpEntity<>(loginBody, loginHeaders),
                java.util.Map.class
        );

        String accessToken = (String) loginResponse.getBody().get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
