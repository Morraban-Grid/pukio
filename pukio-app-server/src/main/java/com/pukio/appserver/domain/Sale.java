package com.pukio.appserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa una transacción de venta.
 * Mapea la tabla "sales" de PostgreSQL.
 */
@Entity
@Table(name = "sales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Long saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @Column(name = "shift_id", length = 50)
    private String shiftId;

    @Column(name = "sale_date")
    private LocalDateTime saleDate;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_total", precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "status", length = 50)
    private String status = "completed";

    // Campos auxiliares para servicios
    @Transient
    private String transactionId;

    @Transient
    private String storeId;

    @Transient
    private BigDecimal igv;

    @Transient
    private BigDecimal totalDiscount;

    @Transient
    private LocalDateTime transactionDate;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.saleDate == null) {
            this.saleDate = LocalDateTime.now();
        }
    }

    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setSale(this);
    }

    // Helpers para acceso desde servicios
    public String getTransactionId() {
        return transactionId != null ? transactionId : (saleId != null ? "TXN-" + saleId : null);
    }

    public String getStoreId() {
        return store != null ? store.getStoreId() : storeId;
    }

    public BigDecimal getIgv() {
        return igv != null ? igv : taxTotal;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount != null ? totalDiscount : discountTotal;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate != null ? transactionDate : saleDate;
    }
}
