package com.pukio.appserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa el inventario de un producto en una tienda.
 * Mapea la tabla "inventory" de PostgreSQL.
 */
@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uq_inventory_sku_store", columnNames = {"sku", "store_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reorder_point")
    private Integer reorderPoint = 10;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "last_updated_by", length = 50)
    private String lastUpdatedBy;

    // Campos auxiliares para servicios
    @Transient
    private String sku;

    @Transient
    private String storeId;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Helpers para acceso desde servicios
    public String getSku() {
        return product != null ? product.getSku() : sku;
    }

    public String getStoreId() {
        return store != null ? store.getStoreId() : storeId;
    }
}
