package com.pukio.posclient.dto;

import java.math.BigDecimal;

/**
 * DTO para solicitud de pago.
 */
public class PaymentRequest {
    private String method;
    private BigDecimal amount;
    
    public PaymentRequest() {}
    
    public PaymentRequest(String method, BigDecimal amount) {
        this.method = method;
        this.amount = amount;
    }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
