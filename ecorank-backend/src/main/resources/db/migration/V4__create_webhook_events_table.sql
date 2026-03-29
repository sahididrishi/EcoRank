CREATE TABLE webhook_events (
    id           BIGSERIAL       PRIMARY KEY,
    event_id     VARCHAR(255)    UNIQUE NOT NULL,
    provider     VARCHAR(32),
    event_type   VARCHAR(128),
    payload      JSONB,
    processed    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_webhook_events_processed_created ON webhook_events (processed, created_at);
