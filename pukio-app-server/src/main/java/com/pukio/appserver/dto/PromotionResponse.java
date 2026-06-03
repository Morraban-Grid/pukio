package com.pukio.appserver.dto;

import com.pukio.appserver.domain.PromotionType;
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
public class PromotionResponse {
    private Long promoId;
    private String name;
    private PromotionType type;
    private BigDecimal value;
    private BigDecimal minPurchase;
    private String scope;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
}
