CREATE TABLE refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    token_hash  VARCHAR(64)     UNIQUE NOT NULL,
    admin_id    BIGINT          NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
