package com.pukio.appserver.domain;

/**
 * Tipos de promociones disponibles en el sistema.
 */
public enum PromotionType {
    PERCENTAGE("Descuento Porcentual"),
    FIXED_AMOUNT("Monto Fijo de Descuento"),
    BUY_X_GET_Y("Compra X lleva Y");

    private final String description;

    PromotionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
