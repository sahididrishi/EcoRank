package dev.ecorank.backend.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.ecorank.backend.dto.request.CreateCheckoutRequest;
import dev.ecorank.backend.dto.response.ProductResponse;
import dev.ecorank.backend.mapper.ProductMapper;
import dev.ecorank.backend.service.PlayerService;
import dev.ecorank.backend.service.ProductService;
import dev.ecorank.backend.service.StripeService;

@RestController
@RequestMapping("/api/v1/store")
public class StoreController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final StripeService stripeService;
    private final PlayerService playerService;

    public StoreController(ProductService productService,
                           ProductMapper productMapper,
                           StripeService stripeService,
                           PlayerService playerService) {
        this.productService = productService;
        this.productMapper = productMapper;
        this.stripeService = stripeService;
        this.playerService = playerService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        List<ProductResponse> products = productService.getActiveProducts()
                .stream()
                .map(productMapper::toResponse)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(@Valid @RequestBody CreateCheckoutRequest request) {
        // Resolve player UUID from Mojang API if not provided
        CreateCheckoutRequest resolvedRequest = request;
        if (request.playerUuid() == null) {
            java.util.UUID resolved = playerService.resolveMinecraftUuid(request.playerName());
            if (resolved == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Could not find Minecraft player: " + request.playerName()));
            }
            resolvedRequest = new CreateCheckoutRequest(
                    request.productSlug(), resolved, request.playerName(), request.idempotencyKey());
        }
        String checkoutUrl = stripeService.createCheckoutSession(resolvedRequest);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }
}
