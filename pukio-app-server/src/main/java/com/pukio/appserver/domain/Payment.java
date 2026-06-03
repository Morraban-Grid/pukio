package com.pukio.appserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un método de pago de una venta.
 * Soporta pagos divididos (múltiples métodos de pago por venta).
 * Mapea la tabla "payments" de PostgreSQL.
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(name = "method", length = 50, nullable = false)
    private String method;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Campo auxiliar para servicios
    @Transient
    private String paymentMethod;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Helper para acceso desde servicios
    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : method;
    }
}
