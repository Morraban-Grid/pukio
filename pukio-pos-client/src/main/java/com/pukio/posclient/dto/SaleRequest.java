package com.pukio.posclient.dto;

import java.util.List;

/**
 * DTO para solicitud de venta al servidor.
 */
public class SaleRequest {
    private List<SaleItemRequest> items;
    private List<PaymentRequest> payments;
    private String storeId;
    private String cashierId;
    
    // Constructors
    public SaleRequest() {}
    
    // Getters and setters
    public List<SaleItemRequest> getItems() { return items; }
    public void setItems(List<SaleItemRequest> items) { this.items = items; }
    
    public List<PaymentRequest> getPayments() { return payments; }
    public void setPayments(List<PaymentRequest> payments) { this.payments = payments; }
    
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    
    public String getCashierId() { return cashierId; }
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }
    
    /**
     * Inner class para ítem de venta.
     */
    public static class SaleItemRequest {
        private String sku;
        private int quantity;
        
        public SaleItemRequest() {}
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
