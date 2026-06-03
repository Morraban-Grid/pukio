package com.pukio.appserver.exception;

public class ArqueoNotFoundException extends RuntimeException {
    public ArqueoNotFoundException(Long arqueoId) {
        super("Arqueo not found with ID: " + arqueoId);
    }
}
