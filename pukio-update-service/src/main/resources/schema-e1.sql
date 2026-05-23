-- ============================================================
-- Pukio POS — Schema Entregable 1
-- PostgreSQL 18.1
-- ============================================================

CREATE TABLE IF NOT EXISTS products (
    id            BIGSERIAL PRIMARY KEY,
    sku           VARCHAR(100)   NOT NULL,
    store_id      VARCHAR(50)    NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    price         NUMERIC(10, 2) NOT NULL,
    category      VARCHAR(100),
    description   TEXT,
    deleted       BOOLEAN        NOT NULL DEFAULT FALSE,
    synced_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_sku_store UNIQUE (sku, store_id)
);

CREATE INDEX IF NOT EXISTS idx_products_sku      ON products (sku);
CREATE INDEX IF NOT EXISTS idx_products_store_id ON products (store_id);

CREATE TABLE IF NOT EXISTS inventory (
    id            BIGSERIAL PRIMARY KEY,
    sku           VARCHAR(100)   NOT NULL,
    store_id      VARCHAR(50)    NOT NULL,
    quantity      INTEGER        NOT NULL DEFAULT 0,
    out_of_stock  BOOLEAN        NOT NULL DEFAULT FALSE,
    last_updated  TIMESTAMP      NOT NULL,
    synced_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_sku_store UNIQUE (sku, store_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_sku      ON inventory (sku);
CREATE INDEX IF NOT EXISTS idx_inventory_store_id ON inventory (store_id);
