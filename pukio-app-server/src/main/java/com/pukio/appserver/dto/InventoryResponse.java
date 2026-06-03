package com.pukio.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private String sku;
    private String productName;
    private String storeId;
    private Integer quantity;
    private Integer reorderPoint;
    private LocalDateTime lastUpdated;
}
