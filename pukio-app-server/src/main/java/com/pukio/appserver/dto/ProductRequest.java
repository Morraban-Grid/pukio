package com.pukio.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank
    @Size(max = 100)
    private String sku;
    
    @NotBlank
    @Size(max = 255)
    private String name;
    
    @NotNull
    @Positive
    private BigDecimal price;
    
    @NotBlank
    private String category;
    
    private String description;
}
