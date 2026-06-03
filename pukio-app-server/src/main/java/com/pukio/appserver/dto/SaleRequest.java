package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SaleRequest {
    @NotBlank
    private String storeId;
    
    @NotBlank
    private String shiftId;
    
    @NotEmpty
    private List<SaleItemRequest> items;
    
    @NotEmpty
    private List<PaymentRequest> payments;
}
