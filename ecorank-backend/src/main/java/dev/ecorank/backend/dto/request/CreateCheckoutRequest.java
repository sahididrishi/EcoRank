package dev.ecorank.backend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutRequest(
        @NotBlank(message = "Product slug is required")
        String productSlug,

        @NotNull(message = "Player UUID is required")
        UUID playerUuid,

        @NotBlank(message = "Player name is required")
        String playerName,

        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey
) {
}
