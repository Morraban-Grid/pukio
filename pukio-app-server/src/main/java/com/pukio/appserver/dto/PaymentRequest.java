package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull
    private String paymentMethod;
    
    @NotNull
    @Positive
    private BigDecimal amount;
}
