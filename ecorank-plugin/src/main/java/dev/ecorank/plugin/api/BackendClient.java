package dev.ecorank.plugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.PendingOrder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OkHttp client for communicating with the EcoRank backend API.
 * All HTTP calls are made asynchronously via CompletableFutures.
 * Sends X-API-Key and X-Server-Id headers for authentication and multi-server tracking.
 */
public class BackendClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String serverId;
    private final Gson gson;
    private final Logger logger;
    private final ExecutorService executor;

    public BackendClient(ConfigService configService, Logger logger) {
        this.baseUrl = configService.getBackendUrl().replaceAll("/$", "");
        this.apiKey = configService.getBackendApiKey();
        this.serverId = configService.getServerId();
        this.gson = new Gson();
        this.logger = logger;
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "EcoRank-HTTP");
            t.setDaemon(true);
            return t;
        });

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Fetches pending orders from the backend fulfillment queue.
     *
     * @return a future containing the list of pending orders, or an empty list on error
     */
    public CompletableFuture<List<PendingOrder>> fetchPendingOrders() {
        return CompletableFuture.supplyAsync(() -> {
            Request request = newAuthenticatedRequest(baseUrl + "/api/v1/plugin/orders/pending")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.warning("Failed to fetch pending orders: HTTP " + response.code());
                    return Collections.<PendingOrder>emptyList();
                }

                String body = response.body() != null ? response.body().string() : "[]";
                return parsePendingOrders(body);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error fetching pending orders from backend", e);
                return Collections.<PendingOrder>emptyList();
            }
        }, executor);
    }

    /**
     * Confirms that an order has been fulfilled (rank granted/removed).
     *
     * @param orderId the order ID to confirm
     * @return a future that resolves to true if confirmation was accepted
     */
    public CompletableFuture<Boolean> confirmFulfillment(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("orderId", orderId);
            payload.addProperty("serverId", serverId);

            RequestBody requestBody = RequestBody.create(gson.toJson(payload), JSON);
            Request request = newAuthenticatedRequest(
                    baseUrl + "/api/v1/plugin/orders/" + orderId + "/confirm")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.warning("Failed to confirm fulfillment for order " + orderId + ": HTTP " + response.code());
                    return false;
                }
                logger.info("Confirmed fulfillment for order " + orderId);
                return true;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error confirming fulfillment for order " + orderId, e);
                return false;
            }
        }, executor);
    }

    /**
     * Creates a new request builder with the required authentication headers.
     */
    private Request.Builder newAuthenticatedRequest(String url) {
        return new Request.Builder()
                .url(url)
                .header("X-API-Key", apiKey)
                .header("X-Server-Id", serverId)
                .header("Accept", "application/json");
    }

    /**
     * Parses a JSON array of pending orders.
     */
    private List<PendingOrder> parsePendingOrders(String json) {
        List<PendingOrder> orders = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                orders.add(new PendingOrder(
                        obj.get("orderId").getAsString(),
                        UUID.fromString(obj.get("playerUuid").getAsString()),
                        obj.get("productSlug").getAsString(),
                        obj.get("rankGroup").getAsString(),
                        obj.get("action").getAsString(),
                        obj.has("amountCents") ? obj.get("amountCents").getAsInt() : 0
                ));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse pending orders JSON", e);
        }
        return orders;
    }

    /**
     * Shuts down the HTTP client and executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        logger.info("Backend client shut down.");
    }
}
