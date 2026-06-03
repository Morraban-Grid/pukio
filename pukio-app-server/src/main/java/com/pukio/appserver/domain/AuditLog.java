package com.pukio.appserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un log de auditoría de operaciones del sistema.
 * Mapea la tabla "audit_log" de PostgreSQL.
 */
@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "operation", length = 100, nullable = false)
    private String operation;

    @Column(name = "entity", length = 100, nullable = false)
    private String entity;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(name = "log_date", updatable = false)
    private LocalDateTime logDate;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // Campos auxiliares para servicios
    @Transient
    private String userId;

    @Transient
    private String action;

    @Transient
    private String entityType;

    @Transient
    private String details;

    @Transient
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.logDate == null) {
            this.logDate = LocalDateTime.now();
        }
    }

    // Helpers para acceso desde servicios
    public String getUserId() {
        return user != null ? user.getUserId() : userId;
    }

    public String getAction() {
        return action != null ? action : operation;
    }

    public String getEntityType() {
        return entityType != null ? entityType : entity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp != null ? timestamp : logDate;
    }
}
