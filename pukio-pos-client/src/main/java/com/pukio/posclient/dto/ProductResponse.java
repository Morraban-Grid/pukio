package com.pukio.posclient.dto;

import java.math.BigDecimal;

/**
 * DTO para respuesta de producto del servidor.
 */
public class ProductResponse {
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private int stockAvailable;
    private boolean active;
    
    // Constructors
    public ProductResponse() {}
    
    public ProductResponse(String sku, String name, BigDecimal price, int stockAvailable) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stockAvailable = stockAvailable;
        this.active = true;
    }
    
    // Getters and setters
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getStockAvailable() { return stockAvailable; }
    public void setStockAvailable(int stockAvailable) { this.stockAvailable = stockAvailable; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
