package com.pukio.appserver.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock for SKU: " + sku + ". Available: " + available + ", Requested: " + requested);
    }
}
