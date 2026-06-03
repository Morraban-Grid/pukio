package com.pukio.posclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto {
    private String sku;
    private String productName;
    private String storeId;
    private String storeName;
    private int quantity;
    private int reorderPoint;
    private LocalDateTime lastUpdated;
}
