package com.pukio.posclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentRequest {
    private String sku;
    private String storeId;
    private int delta;
    private String reason;
    private String userId;
}
