CREATE TABLE products (
    id          BIGSERIAL       PRIMARY KEY,
    slug        VARCHAR(64)     UNIQUE NOT NULL,
    name        VARCHAR(128)    NOT NULL,
    description TEXT,
    price_cents INTEGER         NOT NULL CHECK (price_cents >= 0),
    rank_group  VARCHAR(64),
    category    VARCHAR(64),
    image_url   VARCHAR(512),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INTEGER         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_active_sort ON products (active, sort_order);
