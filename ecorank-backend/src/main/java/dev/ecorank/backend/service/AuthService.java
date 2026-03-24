package dev.ecorank.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ecorank.backend.config.EcoRankProperties;
import dev.ecorank.backend.dto.request.AdminLoginRequest;
import dev.ecorank.backend.dto.response.AuthResponse;
import dev.ecorank.backend.entity.AdminUser;
import dev.ecorank.backend.entity.RefreshToken;
import dev.ecorank.backend.exception.ResourceNotFoundException;
import dev.ecorank.backend.repository.AdminUserRepository;
import dev.ecorank.backend.repository.RefreshTokenRepository;
import dev.ecorank.backend.security.JwtUtil;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EcoRankProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AdminUserRepository adminUserRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EcoRankProperties properties) {
        this.adminUserRepository = adminUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.properties = properties;
    }

    /**
     * Authenticate admin user and return access token + raw refresh token.
     *
     * @return String array: [0] = serialized AuthResponse info isn't needed,
     *         we return AuthResponse and the raw refresh token separately
     */
    @Transactional
    public LoginResult login(AdminLoginRequest request) {
        AdminUser admin = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(admin.getUsername(), admin.getId());
        long expiresIn = jwtUtil.getAccessTokenExpirySeconds();

        // Generate refresh token
        String rawRefreshToken = generateRawToken();
        String tokenHash = hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plus(properties.getJwt().getRefreshTokenExpiryDays(), ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(tokenHash, admin.getId(), expiresAt);
        refreshTokenRepository.save(refreshToken);

        AuthResponse authResponse = new AuthResponse(accessToken, expiresIn);
        return new LoginResult(authResponse, rawRefreshToken);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (existing.getRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        // Revoke the old token (rotation)
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Find the admin
        AdminUser admin = adminUserRepository.findById(existing.getAdminId())
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", existing.getAdminId()));

        // Issue new tokens
        String accessToken = jwtUtil.generateAccessToken(admin.getUsername(), admin.getId());
        long expiresIn = jwtUtil.getAccessTokenExpirySeconds();

        String newRawRefreshToken = generateRawToken();
        String newTokenHash = hashToken(newRawRefreshToken);
        Instant expiresAt = Instant.now().plus(properties.getJwt().getRefreshTokenExpiryDays(), ChronoUnit.DAYS);

        RefreshToken newRefreshToken = new RefreshToken(newTokenHash, admin.getId(), expiresAt);
        refreshTokenRepository.save(newRefreshToken);

        AuthResponse authResponse = new AuthResponse(accessToken, expiresIn);
        return new LoginResult(authResponse, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record LoginResult(AuthResponse authResponse, String rawRefreshToken) {
    }
}
