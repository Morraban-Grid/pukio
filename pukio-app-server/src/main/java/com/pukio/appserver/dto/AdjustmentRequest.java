package com.pukio.appserver.dto;

import com.pukio.appserver.domain.AdjustmentReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentRequest {
    @NotBlank
    private String sku;
    
    @NotBlank
    private String storeId;
    
    @NotNull
    private Integer delta;
    
    @NotNull
    private AdjustmentReason reason;
    
    @NotBlank
    private String userId;
}
