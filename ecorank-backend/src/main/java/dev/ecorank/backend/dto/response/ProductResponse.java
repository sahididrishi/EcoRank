package dev.ecorank.backend.dto.response;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String slug,
        String name,
        String description,
        Integer priceCents,
        String rankGroup,
        String category,
        String imageUrl,
        Boolean active,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
