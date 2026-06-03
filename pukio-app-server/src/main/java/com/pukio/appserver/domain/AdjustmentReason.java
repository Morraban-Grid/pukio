package com.pukio.appserver.domain;

/**
 * Motivos para ajustes manuales de inventario.
 */
public enum AdjustmentReason {
    CORRECCION("Corrección de inventario"),
    MERMA("Merma o pérdida de producto"),
    ROBO("Robo o hurto"),
    DEVOLUCION("Devolución de cliente");

    private final String description;

    AdjustmentReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
