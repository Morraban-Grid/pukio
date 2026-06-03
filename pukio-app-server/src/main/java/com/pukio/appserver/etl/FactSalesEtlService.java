package com.pukio.appserver.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Servicio ETL (Extract, Transform, Load) para poblar la tabla fact_sales
 * del Data Warehouse a partir de ventas transaccionales.
 * 
 * Este servicio se ejecuta de forma asíncrona para no bloquear las operaciones
 * transaccionales del Application Server. Si el DW no está disponible, se loggea
 * el error pero no se afecta la transacción principal.
 * 
 * REQ 2.7 — Application_Server SHALL populate Analytics_Server tables when transactional data is committed.
 * REQ 4.3 — ETL operations SHALL be asynchronous and non-blocking.
 */
@Service
public class FactSalesEtlService {

    private static final Logger log = LoggerFactory.getLogger(FactSalesEtlService.class);
    
    private final JdbcTemplate analyticsJdbcTemplate;

    public FactSalesEtlService(
            @Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate) {
        this.analyticsJdbcTemplate = analyticsJdbcTemplate;
    }

    /**
     * Publica una venta al Data Warehouse de forma asíncrona.
     * 
     * Este método resuelve las claves surrogate de las dimensiones y luego
     * inserta un registro en fact_sales por cada ítem de la venta.
     * 
     * @param saleResponse respuesta de la venta procesada (contiene ID, fecha, ítems, pagos)
     * @param storeId identificador de la tienda donde se realizó la venta
     */
    @Async
    public void publishSale(SaleResponse saleResponse, String storeId) {
        try {
            log.debug("Iniciando ETL para venta ID: {} en tienda: {}", 
                saleResponse.getTransactionId(), storeId);
            
            // Paso 1: Resolver time_id
            Long timeId = resolveTimeId(saleResponse.getSaleDate());
            if (timeId == null) {
                log.error("No se pudo resolver time_id para fecha: {}. Abortando ETL.", 
                    saleResponse.getSaleDate());
                return;
            }
            
            // Paso 2: Resolver store_key
            Long storeKey = resolveStoreKey(storeId);
            if (storeKey == null) {
                log.error("No se pudo resolver store_key para tienda: {}. Abortando ETL.", 
                    storeId);
                return;
            }
            
            // Paso 3: Por cada ítem de la venta, insertar en fact_sales
            for (SaleItemDto item : saleResponse.getItems()) {
                processLineItem(
                    saleResponse.getTransactionId(),
                    timeId,
                    storeKey,
                    item,
                    saleResponse.getPayments()
                );
            }
            
            log.info("ETL completado exitosamente para venta ID: {}", 
                saleResponse.getTransactionId());
            
        } catch (Exception e) {
            // NO lanzar excepción: el ETL es asíncrono y no debe afectar la transacción principal
            log.error("Error en ETL para venta ID: {}. Error: {}", 
                saleResponse.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Resuelve el time_id a partir de la fecha de venta.
     * 
     * @param saleDate fecha de la venta
     * @return time_id de dim_time, o null si no se encuentra
     */
    private Long resolveTimeId(LocalDateTime saleDate) {
        LocalDate date = saleDate.toLocalDate();
        String sql = "SELECT time_id FROM dim_time WHERE full_date = ?";
        
        try {
            return analyticsJdbcTemplate.queryForObject(
                sql, 
                Long.class, 
                Date.valueOf(date)
            );
        } catch (Exception e) {
            log.error("Error resolviendo time_id para fecha {}: {}", date, e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve el store_key a partir del store_id.
     * 
     * @param storeId identificador de la tienda
     * @return store_key de dim_store donde is_current = true, o null si no se encuentra
     */
    private Long resolveStoreKey(String storeId) {
        String sql = "SELECT store_key FROM dim_store WHERE store_id = ? AND is_current = TRUE";
        
        try {
            return analyticsJdbcTemplate.queryForObject(
                sql, 
                Long.class, 
                storeId
            );
        } catch (Exception e) {
            log.error("Error resolviendo store_key para tienda {}: {}", storeId, e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve el product_key a partir del SKU.
     * 
     * @param sku identificador del producto
     * @return product_key de dim_product donde is_current = true, o null si no se encuentra
     */
    private Long resolveProductKey(String sku) {
        String sql = "SELECT product_key FROM dim_product WHERE sku = ? AND is_current = TRUE";
        
        try {
            return analyticsJdbcTemplate.queryForObject(
                sql, 
                Long.class, 
                sku
            );
        } catch (Exception e) {
            log.error("Error resolviendo product_key para SKU {}: {}", sku, e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve el payment_key a partir del método de pago.
     * 
     * @param paymentMethod método de pago (CASH, CARD, etc.)
     * @return payment_key de dim_payment, o null si no se encuentra
     */
    private Long resolvePaymentKey(String paymentMethod) {
        String sql = "SELECT payment_key FROM dim_payment WHERE method = ?";
        
        try {
            return analyticsJdbcTemplate.queryForObject(
                sql, 
                Long.class, 
                paymentMethod
            );
        } catch (Exception e) {
            log.error("Error resolviendo payment_key para método {}: {}", 
                paymentMethod, e.getMessage());
            return null;
        }
    }

    /**
     * Procesa un ítem de venta y lo inserta en fact_sales.
     * 
     * Si el ítem tiene múltiples métodos de pago, se crea un registro por cada método
     * distribuyendo proporcionalmente el monto.
     * 
     * @param saleId ID de la venta transaccional
     * @param timeId clave de dim_time
     * @param storeKey clave de dim_store
     * @param item ítem de la venta
     * @param payments lista de pagos de la venta
     */
    private void processLineItem(
            Long saleId,
            Long timeId,
            Long storeKey,
            SaleItemDto item,
            java.util.List<PaymentDto> payments) {
        
        // Resolver product_key
        Long productKey = resolveProductKey(item.getSku());
        if (productKey == null) {
            log.warn("SKU {} no encontrado en dim_product. Omitiendo ítem.", item.getSku());
            return;
        }
        
        // Si hay múltiples pagos, distribuir proporcionalmente
        if (payments.size() == 1) {
            // Caso simple: un solo método de pago
            insertFactSales(
                saleId,
                timeId,
                productKey,
                storeKey,
                resolvePaymentKey(payments.get(0).getMethod()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountAmount(),
                item.getTaxAmount(),
                item.getLineTotal()
            );
        } else {
            // Caso complejo: múltiples métodos de pago
            BigDecimal totalSale = payments.stream()
                .map(PaymentDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            for (PaymentDto payment : payments) {
                BigDecimal proportion = payment.getAmount().divide(totalSale, 4, BigDecimal.ROUND_HALF_UP);
                BigDecimal proportionalLineTotal = item.getLineTotal().multiply(proportion);
                BigDecimal proportionalDiscount = item.getDiscountAmount().multiply(proportion);
                BigDecimal proportionalTax = item.getTaxAmount().multiply(proportion);
                
                insertFactSales(
                    saleId,
                    timeId,
                    productKey,
                    storeKey,
                    resolvePaymentKey(payment.getMethod()),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    proportionalDiscount,
                    proportionalTax,
                    proportionalLineTotal
                );
            }
        }
    }

    /**
     * Inserta un registro en la tabla fact_sales del Data Warehouse.
     * 
     * @param saleId ID de la venta transaccional
     * @param timeId clave de dim_time
     * @param productKey clave de dim_product
     * @param storeKey clave de dim_store
     * @param paymentKey clave de dim_payment
     * @param quantity cantidad vendida
     * @param unitPrice precio unitario
     * @param discountAmount descuento aplicado
     * @param taxAmount impuesto
     * @param lineTotal total de la línea
     */
    private void insertFactSales(
            Long saleId,
            Long timeId,
            Long productKey,
            Long storeKey,
            Long paymentKey,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal lineTotal) {
        
        if (paymentKey == null) {
            log.warn("payment_key es null. Omitiendo inserción en fact_sales.");
            return;
        }
        
        String sql = """
            INSERT INTO fact_sales (
                sale_id,
                time_id,
                product_key,
                store_key,
                payment_key,
                quantity,
                unit_price,
                discount_amount,
                tax_amount,
                line_total,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try {
            analyticsJdbcTemplate.update(
                sql,
                saleId,
                timeId,
                productKey,
                storeKey,
                paymentKey,
                quantity,
                unitPrice,
                discountAmount,
                taxAmount,
                lineTotal,
                Timestamp.valueOf(LocalDateTime.now())
            );
            
            log.debug("Registro insertado en fact_sales: sale_id={}, product_key={}, payment_key={}", 
                saleId, productKey, paymentKey);
            
        } catch (Exception e) {
            log.error("Error insertando en fact_sales: {}", e.getMessage(), e);
        }
    }

    // ========== DTOs auxiliares (deben coincidir con los del SaleService) ==========
    
    public static class SaleResponse {
        private Long transactionId;
        private LocalDateTime saleDate;
        private java.util.List<SaleItemDto> items;
        private java.util.List<PaymentDto> payments;
        
        // Getters y setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public LocalDateTime getSaleDate() { return saleDate; }
        public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
        public java.util.List<SaleItemDto> getItems() { return items; }
        public void setItems(java.util.List<SaleItemDto> items) { this.items = items; }
        public java.util.List<PaymentDto> getPayments() { return payments; }
        public void setPayments(java.util.List<PaymentDto> payments) { this.payments = payments; }
    }
    
    public static class SaleItemDto {
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
        
        // Getters y setters
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    }
    
    public static class PaymentDto {
        private String method;
        private BigDecimal amount;
        
        // Getters y setters
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
