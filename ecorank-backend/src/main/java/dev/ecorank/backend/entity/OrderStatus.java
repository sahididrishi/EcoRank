package dev.ecorank.backend.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    QUEUED,
    FULFILLED,
    REFUNDED,
    FAILED
}
