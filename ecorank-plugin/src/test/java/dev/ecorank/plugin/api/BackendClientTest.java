package dev.ecorank.plugin.api;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.PendingOrder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackendClientTest {

    private MockWebServer server;
    private BackendClient client;
    private static final Logger LOGGER = Logger.getLogger(BackendClientTest.class.getName());

    @Mock
    private ConfigService configService;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString();
        // Remove trailing slash to match BackendClient behavior
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        when(configService.getBackendUrl()).thenReturn(baseUrl);
        when(configService.getBackendApiKey()).thenReturn("test-api-key");
        when(configService.getServerId()).thenReturn("test-server");

        client = new BackendClient(configService, LOGGER);
    }

    @AfterEach
    void tearDown() throws Exception {
        client.shutdown();
        server.shutdown();
    }

    // --- fetchPendingOrders ---

    @Test
    @DisplayName("fetchPendingOrders returns parsed orders from backend")
    void testFetchPendingOrders() throws Exception {
        String responseBody = """
                [
                    {
                        "orderId": "order-123",
                        "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
                        "productSlug": "vip-rank",
                        "rankGroup": "vip",
                        "action": "GRANT",
                        "amountCents": 999
                    },
                    {
                        "orderId": "order-456",
                        "playerUuid": "660e8400-e29b-41d4-a716-446655440000",
                        "productSlug": "mvp-rank",
                        "rankGroup": "mvp",
                        "action": "REMOVE",
                        "amountCents": 1999
                    }
                ]
                """;

        server.enqueue(new MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        List<PendingOrder> orders = client.fetchPendingOrders().get(5, TimeUnit.SECONDS);

        assertEquals(2, orders.size());

        PendingOrder first = orders.get(0);
        assertEquals("order-123", first.orderId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", first.playerUuid().toString());
        assertEquals("vip-rank", first.productSlug());
        assertEquals("vip", first.rankGroup());
        assertEquals("GRANT", first.action());
        assertEquals(999, first.amountCents());
        assertTrue(first.isGrant());
        assertFalse(first.isRemoval());

        PendingOrder second = orders.get(1);
        assertEquals("order-456", second.orderId());
        assertEquals("REMOVE", second.action());
        assertTrue(second.isRemoval());
        assertFalse(second.isGrant());
    }

    @Test
    @DisplayName("fetchPendingOrders sends correct auth headers")
    void testFetchPendingOrdersHeaders() throws Exception {
        server.enqueue(new MockResponse().setBody("[]").setResponseCode(200));

        client.fetchPendingOrders().get(5, TimeUnit.SECONDS);

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("test-api-key", request.getHeader("X-API-Key"));
        assertEquals("test-server", request.getHeader("X-Server-Id"));
        assertEquals("application/json", request.getHeader("Accept"));
        assertEquals("GET", request.getMethod());
        assertTrue(request.getPath().contains("/api/v1/plugin/orders/pending"));
    }

    @Test
    @DisplayName("fetchPendingOrders returns empty list on HTTP error")
    void testFetchPendingOrdersError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        List<PendingOrder> orders = client.fetchPendingOrders().get(5, TimeUnit.SECONDS);
        assertTrue(orders.isEmpty());
    }

    @Test
    @DisplayName("fetchPendingOrders returns empty list on empty response")
    void testFetchPendingOrdersEmpty() throws Exception {
        server.enqueue(new MockResponse().setBody("[]").setResponseCode(200));

        List<PendingOrder> orders = client.fetchPendingOrders().get(5, TimeUnit.SECONDS);
        assertTrue(orders.isEmpty());
    }

    @Test
    @DisplayName("fetchPendingOrders returns empty list on malformed JSON")
    void testFetchPendingOrdersMalformedJson() throws Exception {
        server.enqueue(new MockResponse().setBody("not valid json").setResponseCode(200));

        List<PendingOrder> orders = client.fetchPendingOrders().get(5, TimeUnit.SECONDS);
        assertTrue(orders.isEmpty());
    }

    // --- confirmFulfillment ---

    @Test
    @DisplayName("confirmFulfillment returns true on 200 response")
    void testConfirmFulfillmentSuccess() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"ok\"}"));

        boolean result = client.confirmFulfillment("order-123").get(5, TimeUnit.SECONDS);
        assertTrue(result);

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/api/v1/plugin/orders/order-123/confirm"));
        assertEquals("test-api-key", request.getHeader("X-API-Key"));

        // Verify request body contains orderId
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("order-123"));
    }

    @Test
    @DisplayName("confirmFulfillment returns false on HTTP error")
    void testConfirmFulfillmentError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        boolean result = client.confirmFulfillment("order-999").get(5, TimeUnit.SECONDS);
        assertFalse(result);
    }

    @Test
    @DisplayName("confirmFulfillment returns false on server error")
    void testConfirmFulfillmentServerError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        boolean result = client.confirmFulfillment("order-123").get(5, TimeUnit.SECONDS);
        assertFalse(result);
    }

    @Test
    @DisplayName("fetchPendingOrders handles missing amountCents gracefully")
    void testFetchPendingOrdersMissingAmountCents() throws Exception {
        String responseBody = """
                [
                    {
                        "orderId": "order-789",
                        "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
                        "productSlug": "basic-rank",
                        "rankGroup": "basic",
                        "action": "GRANT"
                    }
                ]
                """;

        server.enqueue(new MockResponse().setBody(responseBody).setResponseCode(200));

        List<PendingOrder> orders = client.fetchPendingOrders().get(5, TimeUnit.SECONDS);
        assertEquals(1, orders.size());
        assertEquals(0, orders.get(0).amountCents()); // Default when missing
    }
}
