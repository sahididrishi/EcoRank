CREATE TABLE players (
    id         BIGSERIAL       PRIMARY KEY,
    minecraft_uuid UUID        UNIQUE NOT NULL,
    username   VARCHAR(16),
    email      VARCHAR(255),
    created_at TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_minecraft_uuid ON players (minecraft_uuid);
