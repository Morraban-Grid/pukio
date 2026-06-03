package com.pukio.appserver.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SaleResponse {
    private String transactionId;
    private String status;
    private LocalDateTime timestamp;
    private List<SaleItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal grandTotal;
    private BigDecimal totalDiscount;
    private List<PaymentRequest> payments;
}
