package com.pukio.appserver.exception;

import java.math.BigDecimal;

public class InvalidPaymentException extends RuntimeException {
    public InvalidPaymentException(BigDecimal expected, BigDecimal received) {
        super(String.format("Payment amount mismatch: expected=%s, received=%s", expected, received));
    }
    
    public InvalidPaymentException(String message) {
        super(message);
    }
}
