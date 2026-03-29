CREATE TABLE admin_users (
    id            BIGSERIAL       PRIMARY KEY,
    username      VARCHAR(64)     UNIQUE NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    role          VARCHAR(32)     NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
