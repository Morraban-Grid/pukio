package com.pukio.appserver.business.exception;

/**
 * Excepción lanzada cuando no hay suficiente stock para completar una venta.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
