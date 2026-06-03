package com.pukio.posclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransferDto {
    private String sku;
    private String storeOrigenId;
    private String storeDestinoId;
    private int quantity;
    private String userId;
}
