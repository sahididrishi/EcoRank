package dev.ecorank.backend.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateProductRequest(
        String name,

        String slug,

        @Min(value = 1, message = "Price must be at least 1 cent")
        Integer priceCents,

        String description,

        String rankGroup,

        String category,

        String imageUrl,

        Integer sortOrder
) {
}
