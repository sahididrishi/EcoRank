package dev.ecorank.plugin.model;

/**
 * Types of economy transactions tracked in the transaction log.
 */
public enum TransactionType {
    /** Coins earned from killing a mob. */
    MOB_KILL,
    /** Coins earned from daily login bonus. */
    DAILY_LOGIN,
    /** Coins earned from a chest quest. */
    CHEST_QUEST,
    /** Coins transferred between players via /pay. */
    PLAYER_PAY,
    /** Coins granted by an admin via /eco give. */
    ADMIN_GIVE,
    /** Coins removed by an admin via /eco take. */
    ADMIN_TAKE,
    /** Balance set directly by an admin via /eco set. */
    ADMIN_SET,
    /** Coins spent on a rank purchase from the web store. */
    RANK_PURCHASE
}
