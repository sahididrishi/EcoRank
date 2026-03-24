package dev.ecorank.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        @NotBlank(message = "Product slug is required")
        String slug,

        @NotNull(message = "Price is required")
        @Min(value = 1, message = "Price must be at least 1 cent")
        Integer priceCents,

        String description,

        String rankGroup,

        String category,

        String imageUrl
) {
}
