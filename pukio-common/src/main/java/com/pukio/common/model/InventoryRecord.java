package com.pukio.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sku;
    private int quantity;
    private boolean outOfStock;
    private LocalDateTime lastUpdated;
}
