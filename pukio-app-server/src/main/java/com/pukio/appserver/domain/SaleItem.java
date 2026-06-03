package com.pukio.appserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad JPA que representa un item individual dentro de una venta.
 * Mapea la tabla "sale_items" de PostgreSQL.
 */
@Entity
@Table(name = "sale_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    // Campos auxiliares para servicios
    @Transient
    private String sku;

    @Transient
    private String productName;

    @Transient
    private BigDecimal discount;

    @Transient
    private BigDecimal subtotal;

    // Helpers para acceso desde servicios
    public String getSku() {
        return product != null ? product.getSku() : sku;
    }

    public String getProductName() {
        return product != null ? product.getName() : productName;
    }

    public BigDecimal getDiscount() {
        return discount != null ? discount : discountAmount;
    }

    public BigDecimal getSubtotal() {
        return subtotal != null ? subtotal : lineTotal;
    }
}
