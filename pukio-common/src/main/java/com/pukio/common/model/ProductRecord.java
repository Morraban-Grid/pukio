package com.pukio.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sku;
    private String name;
    private BigDecimal price;
    private String category;
    private String description;
    private boolean deleted;
}
