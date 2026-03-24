package dev.ecorank.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ecorank.backend.dto.response.DashboardStatsResponse;
import dev.ecorank.backend.entity.Order;
import dev.ecorank.backend.entity.OrderStatus;
import dev.ecorank.backend.entity.Player;
import dev.ecorank.backend.entity.Product;
import dev.ecorank.backend.exception.ResourceNotFoundException;
import dev.ecorank.backend.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PlayerService playerService;
    private final FulfillmentQueueService fulfillmentQueueService;

    public OrderService(OrderRepository orderRepository,
                        PlayerService playerService,
                        FulfillmentQueueService fulfillmentQueueService) {
        this.orderRepository = orderRepository;
        this.playerService = playerService;
        this.fulfillmentQueueService = fulfillmentQueueService;
    }

    @Transactional
    public Order createOrder(Player player, Product product, UUID idempotencyKey, String provider) {
        // Check idempotency — return existing order if already created
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    Order order = new Order();
                    order.setPlayer(player);
                    order.setProduct(product);
                    order.setIdempotencyKey(idempotencyKey);
                    order.setAmountCents(product.getPriceCents()); // Snapshot price at purchase time
                    order.setStatus(OrderStatus.PENDING_PAYMENT);
                    order.setPaymentProvider(provider);
                    return orderRepository.save(order);
                });
    }

    @Transactional
    public Order markPaid(Long orderId, String provider, String providerPaymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Order {} already in status {}, skipping markPaid", orderId, order.getStatus());
            return order;
        }

        order.setStatus(OrderStatus.QUEUED);
        order.setPaymentProvider(provider);
        order.setProviderPaymentId(providerPaymentId);
        Order saved = orderRepository.save(order);

        // Push to fulfillment queue
        fulfillmentQueueService.enqueueOrder(saved);

        log.info("Order {} marked as QUEUED for fulfillment (provider: {}, paymentId: {})",
                orderId, provider, providerPaymentId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getPendingOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.QUEUED);
    }

    @Transactional
    public Order confirmFulfillment(Long orderId, String serverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.QUEUED) {
            log.warn("Order {} is in status {}, cannot confirm fulfillment", orderId, order.getStatus());
            return order;
        }

        order.setStatus(OrderStatus.FULFILLED);
        order.setServerId(serverId);
        order.setFulfilledAt(Instant.now());

        log.info("Order {} fulfilled by server {}", orderId, serverId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markRefunded(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        order.setStatus(OrderStatus.REFUNDED);
        Order saved = orderRepository.save(order);

        // Queue rank removal
        fulfillmentQueueService.enqueueRankRemoval(saved);

        log.info("Order {} marked as REFUNDED, rank removal queued", orderId);
        return saved;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalRevenue = orderRepository.sumAmountCentsByStatusIn(
                List.of(OrderStatus.PAID, OrderStatus.QUEUED, OrderStatus.FULFILLED)
        );
        long totalOrders = orderRepository.count();
        long totalPlayers = playerService.getTotalPlayers();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.QUEUED);

        return new DashboardStatsResponse(totalRevenue, totalOrders, totalPlayers, pendingOrders);
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrderHistory(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional(readOnly = true)
    public List<Order> getPlayerOrders(UUID playerUuid) {
        return orderRepository.findByPlayerUuid(playerUuid);
    }

    @Transactional(readOnly = true)
    public Order findByProviderPaymentId(String providerPaymentId) {
        return orderRepository.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "providerPaymentId=" + providerPaymentId));
    }
}
