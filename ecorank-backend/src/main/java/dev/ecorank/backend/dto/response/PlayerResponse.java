package dev.ecorank.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PlayerResponse(
        Long id,
        UUID minecraftUuid,
        String username,
        Instant createdAt
) {
}
