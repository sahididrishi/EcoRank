package dev.ecorank.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.ecorank.backend.dto.request.AdminLoginRequest;
import dev.ecorank.backend.dto.request.CreateProductRequest;
import dev.ecorank.backend.dto.request.UpdateProductRequest;
import dev.ecorank.backend.dto.response.AuthResponse;
import dev.ecorank.backend.dto.response.DashboardStatsResponse;
import dev.ecorank.backend.dto.response.OrderResponse;
import dev.ecorank.backend.dto.response.PlayerResponse;
import dev.ecorank.backend.dto.response.ProductResponse;
import dev.ecorank.backend.mapper.OrderMapper;
import dev.ecorank.backend.mapper.PlayerMapper;
import dev.ecorank.backend.mapper.ProductMapper;
import dev.ecorank.backend.service.AuthService;
import dev.ecorank.backend.service.OrderService;
import dev.ecorank.backend.service.PlayerService;
import dev.ecorank.backend.service.ProductService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

    private final AuthService authService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PlayerService playerService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final PlayerMapper playerMapper;

    public AdminController(AuthService authService,
                           OrderService orderService,
                           ProductService productService,
                           PlayerService playerService,
                           OrderMapper orderMapper,
                           ProductMapper productMapper,
                           PlayerMapper playerMapper) {
        this.authService = authService;
        this.orderService = orderService;
        this.productService = productService;
        this.playerService = playerService;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.playerMapper = playerMapper;
    }

    // ===== Auth Endpoints =====

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                               HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        AuthService.LoginResult result = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    // ===== Dashboard =====

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }

    // ===== Orders =====

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<OrderResponse> orders = orderService.getOrderHistory(pageable)
                .map(orderMapper::toResponse);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        var order = orderService.getOrderById(id);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @PostMapping("/orders/{id}/refund")
    public ResponseEntity<OrderResponse> refundOrder(@PathVariable Long id) {
        var order = orderService.markRefunded(id);
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    // ===== Products =====

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts()
                .stream()
                .map(productMapper::toResponse)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        var product = productService.createProduct(request);
        return ResponseEntity.status(201).body(productMapper.toResponse(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateProductRequest request) {
        var product = productService.updateProduct(id, request);
        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @PatchMapping("/products/{id}/active")
    public ResponseEntity<ProductResponse> toggleProductActive(@PathVariable Long id,
                                                                @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            throw new IllegalArgumentException("'active' field is required");
        }
        var product = productService.toggleProductActive(id, active);
        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    // ===== Players =====

    @GetMapping("/players")
    public ResponseEntity<List<PlayerResponse>> searchPlayers(@RequestParam(required = false) String q) {
        List<PlayerResponse> players = playerService.searchPlayers(q)
                .stream()
                .map(playerMapper::toResponse)
                .toList();
        return ResponseEntity.ok(players);
    }

    @GetMapping("/players/{uuid}")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable UUID uuid) {
        var player = playerService.getPlayerByUuid(uuid);
        return ResponseEntity.ok(playerMapper.toResponse(player));
    }

    // ===== Cookie helpers =====

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/admin/auth");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/admin/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
