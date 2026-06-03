package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String type;
    
    @NotNull
    private BigDecimal value;
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    private BigDecimal minimumPurchase;
    private Integer buyQuantity;
    private Integer getQuantity;
    private String applicableSkus;
}
