CREATE TABLE orders (
    id                  BIGSERIAL       PRIMARY KEY,
    player_id           BIGINT          NOT NULL REFERENCES players(id),
    product_id          BIGINT          NOT NULL REFERENCES products(id),
    idempotency_key     UUID            UNIQUE NOT NULL,
    amount_cents        INTEGER         NOT NULL CHECK (amount_cents >= 0),
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING_PAYMENT'
                        CHECK (status IN ('PENDING_PAYMENT','PAID','QUEUED','FULFILLED','REFUNDED','FAILED')),
    payment_provider    VARCHAR(32),
    provider_payment_id VARCHAR(255),
    server_id           VARCHAR(64),
    fulfilled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_player_status ON orders (player_id, status);
CREATE INDEX idx_orders_provider_payment_id ON orders (provider_payment_id);
CREATE INDEX idx_orders_status ON orders (status);
