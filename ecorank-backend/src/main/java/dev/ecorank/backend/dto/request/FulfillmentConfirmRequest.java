package dev.ecorank.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FulfillmentConfirmRequest(
        @NotBlank(message = "Server ID is required")
        String serverId
) {
}
