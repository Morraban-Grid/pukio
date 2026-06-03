package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank
    private String sku;
    
    @NotBlank
    private String fromStoreId;
    
    @NotBlank
    private String toStoreId;
    
    @Positive
    private Integer quantity;
    
    @NotBlank
    private String userId;
}
