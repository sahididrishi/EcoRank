package dev.ecorank.backend.dto.response;

public record DashboardStatsResponse(
        long totalRevenueCents,
        long totalOrders,
        long totalPlayers,
        long pendingOrders
) {
}
