package com.pukio.posclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {
    private Long promoId;
    private String name;
    private String type;
    private BigDecimal value;
    private BigDecimal minPurchase;
    private String scope;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
}
