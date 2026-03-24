package dev.ecorank.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ecorank.backend.dto.request.FulfillmentConfirmRequest;
import dev.ecorank.backend.dto.response.OrderResponse;
import dev.ecorank.backend.dto.response.PendingOrderResponse;
import dev.ecorank.backend.mapper.OrderMapper;
import dev.ecorank.backend.service.OrderService;
import dev.ecorank.backend.service.PlayerService;

@RestController
@RequestMapping("/api/v1/plugin")
public class PluginController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final PlayerService playerService;

    public PluginController(OrderService orderService,
                            OrderMapper orderMapper,
                            PlayerService playerService) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.playerService = playerService;
    }

    @GetMapping("/orders/pending")
    public ResponseEntity<List<PendingOrderResponse>> getPendingOrders() {
        List<PendingOrderResponse> pending = orderService.getPendingOrders()
                .stream()
                .map(orderMapper::toPendingResponse)
                .toList();
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmFulfillment(
            @PathVariable Long orderId,
            @Valid @RequestBody FulfillmentConfirmRequest request) {

        var order = orderService.confirmFulfillment(orderId, request.serverId());
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    /**
     * Called by the plugin on PlayerJoinEvent to register/update player info.
     */
    @PostMapping("/players/join")
    public ResponseEntity<Map<String, String>> playerJoin(@RequestBody Map<String, String> body) {
        String uuidStr = body.get("uuid");
        String username = body.get("username");

        if (uuidStr == null || username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "uuid and username are required"));
        }

        UUID playerUuid = UUID.fromString(uuidStr);
        playerService.getOrCreatePlayer(playerUuid, username);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
