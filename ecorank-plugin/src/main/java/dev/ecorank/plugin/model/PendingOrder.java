package dev.ecorank.plugin.model;

import java.util.UUID;

/**
 * Represents a pending order fetched from the backend fulfillment queue.
 *
 * @param orderId     unique order identifier from the backend
 * @param playerUuid  the Minecraft UUID of the purchasing player
 * @param productSlug the product's URL-friendly identifier
 * @param rankGroup   the LuckPerms group to grant or remove
 * @param action      the action to perform: "GRANT" or "REMOVE"
 * @param amountCents the price in cents at purchase time (for audit)
 */
public record PendingOrder(
        String orderId,
        UUID playerUuid,
        String productSlug,
        String rankGroup,
        String action,
        int amountCents
) {
    public PendingOrder {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId must not be blank");
        if (playerUuid == null) throw new IllegalArgumentException("playerUuid must not be null");
        if (productSlug == null || productSlug.isBlank()) throw new IllegalArgumentException("productSlug must not be blank");
        if (rankGroup == null || rankGroup.isBlank()) throw new IllegalArgumentException("rankGroup must not be blank");
        if (action == null || action.isBlank()) throw new IllegalArgumentException("action must not be blank");
    }

    /**
     * Whether this order is a rank grant (as opposed to removal).
     */
    public boolean isGrant() {
        return "GRANT".equalsIgnoreCase(action);
    }

    /**
     * Whether this order is a rank removal (refund-triggered).
     */
    public boolean isRemoval() {
        return "REMOVE".equalsIgnoreCase(action);
    }
}
