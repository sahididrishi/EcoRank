package dev.ecorank.backend.dto.response;

import java.util.UUID;

public record PendingOrderResponse(
        String orderId,
        UUID playerUuid,
        String productSlug,
        String rankGroup,
        String action,
        Integer amountCents
) {
}
