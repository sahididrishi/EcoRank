package dev.ecorank.backend.dto.response;

public record AuthResponse(
        String accessToken,
        long expiresIn
) {
}
