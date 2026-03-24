package dev.ecorank.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ecorank.backend.entity.Order;

/**
 * Manages the Redis fulfillment queue. Uses Redis List via RPUSH so messages persist
 * even if the plugin is offline.
 */
@Service
public class FulfillmentQueueService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentQueueService.class);
    private static final String FULFILLMENT_QUEUE_KEY = "ecorank:fulfillment:queue";
    private static final String RANK_REMOVAL_QUEUE_KEY = "ecorank:fulfillment:rank_removals";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean redisAvailable;

    public FulfillmentQueueService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisAvailable = testRedisConnection();
    }

    private boolean testRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis not available, fulfillment queue will operate in DB-only mode: {}", e.getMessage());
            return false;
        }
    }

    public void enqueueOrder(Order order) {
        if (!redisAvailable) {
            log.debug("Redis unavailable, order {} relies on DB polling only", order.getId());
            return;
        }

        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("orderId", order.getId());
            node.put("playerUuid", order.getPlayer().getMinecraftUuid().toString());
            node.put("productSlug", order.getProduct().getSlug());
            node.put("rankGroup", order.getProduct().getRankGroup());
            node.put("amountCents", order.getAmountCents());
            node.put("action", "GRANT_RANK");

            String json = objectMapper.writeValueAsString(node);
            redisTemplate.opsForList().rightPush(FULFILLMENT_QUEUE_KEY, json);
            log.debug("Order {} enqueued for fulfillment", order.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue order {} to Redis: {}", order.getId(), e.getMessage());
        }
    }

    public void enqueueRankRemoval(Order order) {
        if (!redisAvailable) {
            log.debug("Redis unavailable, rank removal for order {} relies on DB polling only", order.getId());
            return;
        }

        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("orderId", order.getId());
            node.put("playerUuid", order.getPlayer().getMinecraftUuid().toString());
            node.put("rankGroup", order.getProduct().getRankGroup());
            node.put("action", "REMOVE_RANK");

            String json = objectMapper.writeValueAsString(node);
            redisTemplate.opsForList().rightPush(RANK_REMOVAL_QUEUE_KEY, json);
            log.debug("Rank removal for order {} enqueued", order.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue rank removal for order {} to Redis: {}", order.getId(), e.getMessage());
        }
    }
}
