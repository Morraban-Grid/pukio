package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SaleItemRequest {
    @NotBlank
    private String sku;
    
    @Positive
    private int quantity;
}
