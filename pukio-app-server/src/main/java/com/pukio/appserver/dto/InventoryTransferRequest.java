package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InventoryTransferRequest {
    @NotBlank
    private String sku;
    
    @NotBlank
    private String fromStoreId;
    
    @NotBlank
    private String toStoreId;
    
    @Positive
    private int quantity;
}
