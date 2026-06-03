-- ==============================================================================
-- Data Warehouse Schema — Sistema POS Pukio
-- Entregable 2: Esquema Estrella para Análisis de Ventas
-- PostgreSQL 18.1
-- ==============================================================================

-- ==============================================================================
-- Dimensión: Tiempo
-- ==============================================================================
CREATE TABLE IF NOT EXISTS dim_time (
    time_id BIGSERIAL PRIMARY KEY,
    full_date DATE NOT NULL UNIQUE,
    year INT NOT NULL,
    quarter INT NOT NULL CHECK (quarter BETWEEN 1 AND 4),
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    week INT NOT NULL CHECK (week BETWEEN 1 AND 53),
    day_of_month INT NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    is_weekend BOOLEAN NOT NULL DEFAULT FALSE,
    is_holiday BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_dim_time_full_date ON dim_time(full_date);
CREATE INDEX idx_dim_time_year_month ON dim_time(year, month);
CREATE INDEX idx_dim_time_year_quarter ON dim_time(year, quarter);

COMMENT ON TABLE dim_time IS 'Dimensión temporal para análisis de ventas por fecha';
COMMENT ON COLUMN dim_time.time_id IS 'Clave surrogate de la dimensión tiempo';
COMMENT ON COLUMN dim_time.full_date IS 'Fecha completa (Natural Key)';
COMMENT ON COLUMN dim_time.quarter IS 'Trimestre del año (1-4)';
COMMENT ON COLUMN dim_time.week IS 'Semana del año (1-53)';
COMMENT ON COLUMN dim_time.day_of_week IS 'Día de la semana (1=Lunes, 7=Domingo)';
COMMENT ON COLUMN dim_time.is_weekend IS 'TRUE si es sábado o domingo';
COMMENT ON COLUMN dim_time.is_holiday IS 'TRUE si es día feriado';

-- ==============================================================================
-- Dimensión: Producto (Slowly Changing Dimension Type 2)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS dim_product (
    product_key BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    valid_from DATE NOT NULL,
    valid_to DATE,
    is_current BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_dim_product_sku_current ON dim_product(sku) WHERE is_current = TRUE;
CREATE INDEX idx_dim_product_category ON dim_product(category);
CREATE INDEX idx_dim_product_valid_from ON dim_product(valid_from);
CREATE INDEX idx_dim_product_valid_to ON dim_product(valid_to);

COMMENT ON TABLE dim_product IS 'Dimensión de productos con historial de cambios (SCD Type 2)';
COMMENT ON COLUMN dim_product.product_key IS 'Clave surrogate de la dimensión producto';
COMMENT ON COLUMN dim_product.sku IS 'Stock Keeping Unit (Natural Key)';
COMMENT ON COLUMN dim_product.valid_from IS 'Fecha de inicio de vigencia de este registro';
COMMENT ON COLUMN dim_product.valid_to IS 'Fecha de fin de vigencia (NULL si es la versión actual)';
COMMENT ON COLUMN dim_product.is_current IS 'TRUE si es la versión vigente del producto';

-- ==============================================================================
-- Dimensión: Tienda (Slowly Changing Dimension Type 2)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS dim_store (
    store_key BIGSERIAL PRIMARY KEY,
    store_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    region VARCHAR(100),
    valid_from DATE NOT NULL,
    valid_to DATE,
    is_current BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_dim_store_id_current ON dim_store(store_id) WHERE is_current = TRUE;
CREATE INDEX idx_dim_store_region ON dim_store(region);
CREATE INDEX idx_dim_store_city ON dim_store(city);

COMMENT ON TABLE dim_store IS 'Dimensión de tiendas con historial de cambios (SCD Type 2)';
COMMENT ON COLUMN dim_store.store_key IS 'Clave surrogate de la dimensión tienda';
COMMENT ON COLUMN dim_store.store_id IS 'Identificador de tienda (Natural Key)';
COMMENT ON COLUMN dim_store.valid_from IS 'Fecha de inicio de vigencia de este registro';
COMMENT ON COLUMN dim_store.valid_to IS 'Fecha de fin de vigencia (NULL si es la versión actual)';
COMMENT ON COLUMN dim_store.is_current IS 'TRUE si es la versión vigente de la tienda';

-- ==============================================================================
-- Dimensión: Método de Pago
-- ==============================================================================
CREATE TABLE IF NOT EXISTS dim_payment (
    payment_key BIGSERIAL PRIMARY KEY,
    method VARCHAR(50) NOT NULL UNIQUE,
    method_description VARCHAR(255)
);

CREATE INDEX idx_dim_payment_method ON dim_payment(method);

COMMENT ON TABLE dim_payment IS 'Dimensión de métodos de pago';
COMMENT ON COLUMN dim_payment.payment_key IS 'Clave surrogate de la dimensión método de pago';
COMMENT ON COLUMN dim_payment.method IS 'Método de pago (CASH, CARD, TRANSFER, etc.)';

-- Poblar métodos de pago iniciales
INSERT INTO dim_payment (method, method_description) VALUES
    ('CASH', 'Efectivo'),
    ('CREDIT_CARD', 'Tarjeta de Crédito'),
    ('DEBIT_CARD', 'Tarjeta de Débito'),
    ('BANK_TRANSFER', 'Transferencia Bancaria'),
    ('DIGITAL_WALLET', 'Billetera Digital')
ON CONFLICT (method) DO NOTHING;

-- ==============================================================================
-- Tabla de Hechos: Ventas
-- ==============================================================================
CREATE TABLE IF NOT EXISTS fact_sales (
    fact_id BIGSERIAL PRIMARY KEY,
    time_id BIGINT NOT NULL,
    product_key BIGINT NOT NULL,
    store_key BIGINT NOT NULL,
    payment_key BIGINT NOT NULL,
    sale_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    line_total DECIMAL(10, 2) NOT NULL CHECK (line_total >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_fact_sales_time FOREIGN KEY (time_id) 
        REFERENCES dim_time(time_id),
    CONSTRAINT fk_fact_sales_product FOREIGN KEY (product_key) 
        REFERENCES dim_product(product_key),
    CONSTRAINT fk_fact_sales_store FOREIGN KEY (store_key) 
        REFERENCES dim_store(store_key),
    CONSTRAINT fk_fact_sales_payment FOREIGN KEY (payment_key) 
        REFERENCES dim_payment(payment_key)
);

-- Índices para optimizar queries analíticas
CREATE INDEX idx_fact_sales_time_id ON fact_sales(time_id);
CREATE INDEX idx_fact_sales_product_key ON fact_sales(product_key);
CREATE INDEX idx_fact_sales_store_key ON fact_sales(store_key);
CREATE INDEX idx_fact_sales_payment_key ON fact_sales(payment_key);
CREATE INDEX idx_fact_sales_sale_id ON fact_sales(sale_id);
CREATE INDEX idx_fact_sales_created_at ON fact_sales(created_at);

-- Índices compuestos para queries comunes
CREATE INDEX idx_fact_sales_time_store ON fact_sales(time_id, store_key);
CREATE INDEX idx_fact_sales_time_product ON fact_sales(time_id, product_key);
CREATE INDEX idx_fact_sales_store_product ON fact_sales(store_key, product_key);

COMMENT ON TABLE fact_sales IS 'Tabla de hechos de ventas con métricas a nivel de línea de ítem';
COMMENT ON COLUMN fact_sales.fact_id IS 'Clave surrogate de la tabla de hechos';
COMMENT ON COLUMN fact_sales.sale_id IS 'ID de la venta en el sistema transaccional (Natural Key)';
COMMENT ON COLUMN fact_sales.quantity IS 'Cantidad vendida';
COMMENT ON COLUMN fact_sales.unit_price IS 'Precio unitario al momento de la venta';
COMMENT ON COLUMN fact_sales.discount_amount IS 'Monto de descuento aplicado';
COMMENT ON COLUMN fact_sales.tax_amount IS 'Monto de impuesto (IGV 18%)';
COMMENT ON COLUMN fact_sales.line_total IS 'Total de la línea (quantity * unit_price - discount + tax)';
COMMENT ON COLUMN fact_sales.created_at IS 'Timestamp de inserción en el DW (para auditoría ETL)';

-- ==============================================================================
-- Vistas Analíticas
-- ==============================================================================

-- Vista: Ventas por día
CREATE OR REPLACE VIEW v_sales_by_day AS
SELECT 
    dt.full_date,
    dt.year,
    dt.month,
    dt.day_of_month,
    dt.day_of_week,
    dt.is_weekend,
    SUM(fs.quantity) AS total_quantity,
    SUM(fs.line_total) AS total_revenue,
    SUM(fs.discount_amount) AS total_discount,
    SUM(fs.tax_amount) AS total_tax,
    COUNT(DISTINCT fs.sale_id) AS total_transactions
FROM fact_sales fs
JOIN dim_time dt ON fs.time_id = dt.time_id
GROUP BY dt.full_date, dt.year, dt.month, dt.day_of_month, dt.day_of_week, dt.is_weekend
ORDER BY dt.full_date;

-- Vista: Top productos por ventas
CREATE OR REPLACE VIEW v_top_products AS
SELECT 
    dp.sku,
    dp.name,
    dp.category,
    SUM(fs.quantity) AS total_quantity_sold,
    SUM(fs.line_total) AS total_revenue,
    COUNT(DISTINCT fs.sale_id) AS total_transactions,
    AVG(fs.unit_price) AS avg_unit_price
FROM fact_sales fs
JOIN dim_product dp ON fs.product_key = dp.product_key
WHERE dp.is_current = TRUE
GROUP BY dp.sku, dp.name, dp.category
ORDER BY total_revenue DESC;

-- Vista: Ventas por tienda
CREATE OR REPLACE VIEW v_sales_by_store AS
SELECT 
    ds.store_id,
    ds.name AS store_name,
    ds.city,
    ds.region,
    SUM(fs.quantity) AS total_quantity,
    SUM(fs.line_total) AS total_revenue,
    COUNT(DISTINCT fs.sale_id) AS total_transactions,
    AVG(fs.line_total) AS avg_transaction_value
FROM fact_sales fs
JOIN dim_store ds ON fs.store_key = ds.store_key
WHERE ds.is_current = TRUE
GROUP BY ds.store_id, ds.name, ds.city, ds.region
ORDER BY total_revenue DESC;

-- Vista: Ventas por método de pago
CREATE OR REPLACE VIEW v_sales_by_payment_method AS
SELECT 
    dp.method,
    dp.method_description,
    SUM(fs.line_total) AS total_revenue,
    COUNT(DISTINCT fs.sale_id) AS total_transactions,
    AVG(fs.line_total) AS avg_transaction_value
FROM fact_sales fs
JOIN dim_payment dp ON fs.payment_key = dp.payment_key
GROUP BY dp.method, dp.method_description
ORDER BY total_revenue DESC;

-- ==============================================================================
-- Fin del Schema
-- ==============================================================================
