package dev.ecorank.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        Long id,
        String playerName,
        UUID playerUuid,
        String productName,
        String productSlug,
        Integer amountCents,
        String status,
        Instant createdAt,
        Instant fulfilledAt
) {
}
