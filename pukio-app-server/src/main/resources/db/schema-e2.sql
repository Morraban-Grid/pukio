-- ============================================================
-- Pukio POS System — Entregable 2: Arquitectura Cliente/Servidor
-- Schema SQL para PostgreSQL 18.1
-- ============================================================

-- Tabla: stores (tiendas del sistema)
CREATE TABLE IF NOT EXISTS stores (
    store_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    region VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: users (usuarios del sistema con roles)
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('cashier', 'supervisor', 'manager', 'administrator', 'auditor')),
    store_id VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_store FOREIGN KEY (store_id) REFERENCES stores(store_id)
);

-- Tabla: products (catálogo de productos)
CREATE TABLE IF NOT EXISTS products (
    sku VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL CHECK (price >= 0),
    category VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: inventory (inventario por tienda y producto)
CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    store_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reorder_point INT DEFAULT 10,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_by VARCHAR(50),
    CONSTRAINT fk_inventory_product FOREIGN KEY (sku) REFERENCES products(sku),
    CONSTRAINT fk_inventory_store FOREIGN KEY (store_id) REFERENCES stores(store_id),
    CONSTRAINT uq_inventory_sku_store UNIQUE (sku, store_id)
);

-- Tabla: promotions (promociones y descuentos configurables)
CREATE TABLE IF NOT EXISTS promotions (
    promo_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('percentage', 'fixed_amount', 'buy_x_get_y')),
    value DECIMAL(12, 2) NOT NULL,
    min_purchase DECIMAL(12, 2) DEFAULT 0,
    scope VARCHAR(100), -- 'all' o categoria específica o SKU específico
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: sales (transacciones de venta)
CREATE TABLE IF NOT EXISTS sales (
    sale_id BIGSERIAL PRIMARY KEY,
    store_id VARCHAR(50) NOT NULL,
    cashier_id VARCHAR(50),
    shift_id VARCHAR(50),
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(12, 2) NOT NULL CHECK (subtotal >= 0),
    discount_total DECIMAL(12, 2) DEFAULT 0 CHECK (discount_total >= 0),
    tax_total DECIMAL(12, 2) DEFAULT 0 CHECK (tax_total >= 0),
    grand_total DECIMAL(12, 2) NOT NULL CHECK (grand_total >= 0),
    status VARCHAR(50) DEFAULT 'completed' CHECK (status IN ('pending', 'completed', 'cancelled')),
    CONSTRAINT fk_sale_store FOREIGN KEY (store_id) REFERENCES stores(store_id),
    CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) REFERENCES users(user_id)
);

-- Tabla: sale_items (items de cada venta)
CREATE TABLE IF NOT EXISTS sale_items (
    item_id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0),
    discount_amount DECIMAL(12, 2) DEFAULT 0 CHECK (discount_amount >= 0),
    line_total DECIMAL(12, 2) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT fk_saleitem_sale FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE,
    CONSTRAINT fk_saleitem_product FOREIGN KEY (sku) REFERENCES products(sku)
);

-- Tabla: payments (métodos de pago de cada venta, soporte multi-pago)
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    method VARCHAR(50) NOT NULL CHECK (method IN ('cash', 'credit_card', 'debit_card', 'transfer', 'digital_wallet')),
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    reference VARCHAR(255), -- referencia externa (número de autorización, etc.)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_sale FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE
);

-- Tabla: arqueo (cierre de caja por turno y cajero)
CREATE TABLE IF NOT EXISTS arqueo (
    arqueo_id BIGSERIAL PRIMARY KEY,
    store_id VARCHAR(50) NOT NULL,
    cashier_id VARCHAR(50) NOT NULL,
    shift_id VARCHAR(50) NOT NULL,
    arqueo_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cash_expected DECIMAL(12, 2) DEFAULT 0,
    cash_declared DECIMAL(12, 2) DEFAULT 0,
    cash_variance DECIMAL(12, 2) GENERATED ALWAYS AS (cash_declared - cash_expected) STORED,
    card_expected DECIMAL(12, 2) DEFAULT 0,
    card_declared DECIMAL(12, 2) DEFAULT 0,
    card_variance DECIMAL(12, 2) GENERATED ALWAYS AS (card_declared - card_expected) STORED,
    status VARCHAR(50) DEFAULT 'closed' CHECK (status IN ('pending_approval', 'closed', 'rejected')),
    approved_by VARCHAR(50),
    CONSTRAINT fk_arqueo_store FOREIGN KEY (store_id) REFERENCES stores(store_id),
    CONSTRAINT fk_arqueo_cashier FOREIGN KEY (cashier_id) REFERENCES users(user_id)
);

-- Tabla: audit_log (log de auditoría de todas las operaciones del sistema)
CREATE TABLE IF NOT EXISTS audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50),
    operation VARCHAR(100) NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', etc.
    entity VARCHAR(100) NOT NULL, -- 'Product', 'Sale', 'Inventory', etc.
    entity_id VARCHAR(255), -- ID de la entidad afectada
    before_value TEXT, -- JSON del estado anterior (para UPDATE/DELETE)
    after_value TEXT, -- JSON del estado nuevo (para CREATE/UPDATE)
    log_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    CONSTRAINT fk_auditlog_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ============================================================
-- ÍNDICES ADICIONALES PARA OPTIMIZACIÓN DE CONSULTAS
-- ============================================================

-- Índices en products
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);

-- Índices en inventory
CREATE INDEX IF NOT EXISTS idx_inventory_sku ON inventory(sku);
CREATE INDEX IF NOT EXISTS idx_inventory_store ON inventory(store_id);
CREATE INDEX IF NOT EXISTS idx_inventory_quantity ON inventory(quantity);

-- Índices en sales
CREATE INDEX IF NOT EXISTS idx_sales_store ON sales(store_id);
CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_cashier ON sales(cashier_id);
CREATE INDEX IF NOT EXISTS idx_sales_shift ON sales(shift_id);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);

-- Índices en sale_items
CREATE INDEX IF NOT EXISTS idx_saleitems_sale ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_saleitems_sku ON sale_items(sku);

-- Índices en payments
CREATE INDEX IF NOT EXISTS idx_payments_sale ON payments(sale_id);
CREATE INDEX IF NOT EXISTS idx_payments_method ON payments(method);

-- Índices en arqueo
CREATE INDEX IF NOT EXISTS idx_arqueo_store ON arqueo(store_id);
CREATE INDEX IF NOT EXISTS idx_arqueo_cashier ON arqueo(cashier_id);
CREATE INDEX IF NOT EXISTS idx_arqueo_shift ON arqueo(shift_id);
CREATE INDEX IF NOT EXISTS idx_arqueo_date ON arqueo(arqueo_date);

-- Índices en audit_log
CREATE INDEX IF NOT EXISTS idx_auditlog_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_auditlog_date ON audit_log(log_date);
CREATE INDEX IF NOT EXISTS idx_auditlog_entity ON audit_log(entity);
CREATE INDEX IF NOT EXISTS idx_auditlog_operation ON audit_log(operation);

-- ============================================================
-- DATOS DE EJEMPLO INICIALES (opcional para testing)
-- ============================================================

-- Insertar tienda de ejemplo
INSERT INTO stores (store_id, name, city, region, active) 
VALUES ('STORE-001', 'Pukio Lima Norte', 'Lima', 'Lima', TRUE)
ON CONFLICT (store_id) DO NOTHING;

-- Insertar usuario administrador de ejemplo
INSERT INTO users (user_id, username, role, store_id, active)
VALUES ('USR-ADMIN', 'admin', 'administrator', 'STORE-001', TRUE)
ON CONFLICT (user_id) DO NOTHING;

-- Insertar cajero de ejemplo
INSERT INTO users (user_id, username, role, store_id, active)
VALUES ('USR-CASHIER01', 'cajero01', 'cashier', 'STORE-001', TRUE)
ON CONFLICT (user_id) DO NOTHING;

COMMENT ON TABLE stores IS 'Tiendas del sistema POS';
COMMENT ON TABLE users IS 'Usuarios del sistema con roles RBAC';
COMMENT ON TABLE products IS 'Catálogo maestro de productos';
COMMENT ON TABLE inventory IS 'Inventario por tienda y producto';
COMMENT ON TABLE promotions IS 'Promociones y descuentos configurables';
COMMENT ON TABLE sales IS 'Transacciones de venta completas';
COMMENT ON TABLE sale_items IS 'Items individuales de cada venta';
COMMENT ON TABLE payments IS 'Métodos de pago (soporte multi-pago)';
COMMENT ON TABLE arqueo IS 'Cierre de caja por turno';
COMMENT ON TABLE audit_log IS 'Log de auditoría de todas las operaciones';
