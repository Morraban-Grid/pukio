package com.pukio.appserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un arqueo (cierre de caja) por turno.
 * Mapea la tabla "arqueo" de PostgreSQL.
 */
@Entity
@Table(name = "arqueo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Arqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "arqueo_id")
    private Long arqueoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @Column(name = "shift_id", length = 50, nullable = false)
    private String shiftId;

    @Column(name = "arqueo_date")
    private LocalDateTime arqueoDate;

    @Column(name = "cash_expected", precision = 12, scale = 2)
    private BigDecimal cashExpected = BigDecimal.ZERO;

    @Column(name = "cash_declared", precision = 12, scale = 2)
    private BigDecimal cashDeclared = BigDecimal.ZERO;

    // cash_variance es GENERATED ALWAYS en la BD, no se mapea como campo editable
    @Column(name = "cash_variance", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal cashVariance;

    @Column(name = "card_expected", precision = 12, scale = 2)
    private BigDecimal cardExpected = BigDecimal.ZERO;

    @Column(name = "card_declared", precision = 12, scale = 2)
    private BigDecimal cardDeclared = BigDecimal.ZERO;

    // card_variance es GENERATED ALWAYS en la BD
    @Column(name = "card_variance", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal cardVariance;

    @Column(name = "status", length = 50)
    private String status = "closed";

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Campo auxiliar para servicios (storeId directa desde store)
    @Transient
    private String storeId;

    // Campos auxiliares para representar montos esperados/declarados en JSON
    @Transient
    private String expectedAmounts;

    @Transient
    private String declaredAmounts;

    @Transient
    private BigDecimal totalVariance;

    @PrePersist
    protected void onCreate() {
        if (this.arqueoDate == null) {
            this.arqueoDate = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Helpers para acceso directo desde servicios
    public String getStoreId() {
        return store != null ? store.getStoreId() : storeId;
    }

    public BigDecimal getTotalVariance() {
        if (totalVariance != null) return totalVariance;
        if (cashVariance != null && cardVariance != null) {
            return cashVariance.add(cardVariance);
        }
        return BigDecimal.ZERO;
    }
}
