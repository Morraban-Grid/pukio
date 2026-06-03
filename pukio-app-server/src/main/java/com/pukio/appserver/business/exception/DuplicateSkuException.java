package com.pukio.appserver.business.exception;

/**
 * Excepción lanzada cuando se intenta crear un producto con un SKU que ya existe.
 */
public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String message) {
        super(message);
    }
}
