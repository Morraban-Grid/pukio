package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ArqueoRequest {
    @NotBlank
    private String storeId;
    
    @NotBlank
    private String shiftId;
    
    @NotNull
    private Map<String, BigDecimal> declaredAmounts;
}
