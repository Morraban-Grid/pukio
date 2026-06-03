package com.pukio.posclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for inventory data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private String sku;
    private String storeId;
    private int quantity;
    private int reorderPoint;
    private boolean outOfStock;
    private LocalDateTime lastUpdated;
}
