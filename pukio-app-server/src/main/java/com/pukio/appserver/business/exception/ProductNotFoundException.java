package com.pukio.appserver.business.exception;

/**
 * Excepción lanzada cuando no se encuentra un producto por SKU.
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
