package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustmentRequest {
    @NotBlank
    private String sku;
    
    @NotBlank
    private String storeId;
    
    @NotBlank
    private String reason;
    
    @NotNull
    private Integer delta;
}
