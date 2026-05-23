package com.pukio.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private LocalDateTime timestamp;
    private List<LineItem> items;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
}
