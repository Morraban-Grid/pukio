package com.pukio.posclient.ui.sale;

import com.pukio.posclient.dto.ProductDto;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model para la tabla de ítems de venta.
 * Columnas: SKU, Nombre, Cantidad (editable), Precio Unit., Descuento, Subtotal, Eliminar (botón)
 */
public class SaleItemTableModel extends AbstractTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "SKU", "Nombre", "Cantidad", "Precio Unit.", "Descuento", "Subtotal", ""
    };
    
    private static final int COL_SKU = 0;
    private static final int COL_NAME = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_UNIT_PRICE = 3;
    private static final int COL_DISCOUNT = 4;
    private static final int COL_SUBTOTAL = 5;
    private static final int COL_DELETE = 6;
    
    private final List<SaleItemRow> items = new ArrayList<>();
    
    @Override
    public int getRowCount() {
        return items.size();
    }
    
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == COL_QUANTITY) {
            return Integer.class;
        }
        if (columnIndex == COL_UNIT_PRICE || columnIndex == COL_DISCOUNT || columnIndex == COL_SUBTOTAL) {
            return BigDecimal.class;
        }
        return String.class;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Solo la columna Cantidad es editable
        return columnIndex == COL_QUANTITY;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SaleItemRow item = items.get(rowIndex);
        switch (columnIndex) {
            case COL_SKU:
                return item.getSku();
            case COL_NAME:
                return item.getName();
            case COL_QUANTITY:
                return item.getQuantity();
            case COL_UNIT_PRICE:
                return item.getUnitPrice();
            case COL_DISCOUNT:
                return item.getDiscountAmount();
            case COL_SUBTOTAL:
                return item.getSubtotal();
            case COL_DELETE:
                return "Eliminar";
            default:
                return null;
        }
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == COL_QUANTITY) {
            try {
                int newQuantity = Integer.parseInt(aValue.toString());
                if (newQuantity <= 0) {
                    throw new IllegalArgumentException("Cantidad debe ser positiva");
                }
                
                SaleItemRow item = items.get(rowIndex);
                item.setQuantity(newQuantity);
                
                // Recalcular subtotal
                BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(newQuantity))
                    .subtract(item.getDiscountAmount());
                item.setSubtotal(subtotal);
                
                fireTableRowsUpdated(rowIndex, rowIndex);
            } catch (IllegalArgumentException e) {
                // Mantener el valor anterior si la entrada es inválida
                System.err.println("Cantidad inválida: " + e.getMessage());
            }
        }
    }
    
    /**
     * Añade un ítem a la venta. Si el SKU ya existe, suma la cantidad.
     */
    public void addItem(ProductDto product, int quantity) {
        // Buscar si el SKU ya existe
        for (int i = 0; i < items.size(); i++) {
            SaleItemRow existing = items.get(i);
            if (existing.getSku().equals(product.getSku())) {
                // Sumar cantidad
                int newQuantity = existing.getQuantity() + quantity;
                existing.setQuantity(newQuantity);
                
                // Recalcular subtotal
                BigDecimal subtotal = existing.getUnitPrice()
                    .multiply(BigDecimal.valueOf(newQuantity))
                    .subtract(existing.getDiscountAmount());
                existing.setSubtotal(subtotal);
                
                fireTableRowsUpdated(i, i);
                return;
            }
        }
        
        // No existe, crear nuevo ítem
        BigDecimal subtotal = product.getPrice()
            .multiply(BigDecimal.valueOf(quantity));
        
        SaleItemRow newItem = new SaleItemRow(
            product.getSku(),
            product.getName(),
            quantity,
            product.getPrice(),
            BigDecimal.ZERO, // Sin descuento inicial
            subtotal
        );
        
        items.add(newItem);
        fireTableRowsInserted(items.size() - 1, items.size() - 1);
    }
    
    /**
     * Elimina un ítem por índice de fila.
     */
    public void removeItem(int row) {
        if (row >= 0 && row < items.size()) {
            items.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }
    
    /**
     * Limpia todos los ítems de la venta.
     */
    public void clear() {
        int oldSize = items.size();
        items.clear();
        if (oldSize > 0) {
            fireTableRowsDeleted(0, oldSize - 1);
        }
    }
    
    /**
     * Retorna los totales calculados de la venta actual.
     */
    public SaleTotals getTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        
        for (SaleItemRow item : items) {
            subtotal = subtotal.add(item.getSubtotal());
            discountTotal = discountTotal.add(item.getDiscountAmount());
        }
        
        // IGV 18%
        BigDecimal taxTotal = subtotal.multiply(new BigDecimal("0.18"));
        BigDecimal grandTotal = subtotal.add(taxTotal);
        
        return new SaleTotals(subtotal, discountTotal, taxTotal, grandTotal);
    }
    
    /**
     * Retorna la lista de ítems (para enviar al servidor).
     */
    public List<SaleItemRow> getItems() {
        return new ArrayList<>(items);
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Clase interna para representar una fila de ítem de venta.
     */
    public static class SaleItemRow {
        private String sku;
        private String name;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal subtotal;
        
        public SaleItemRow(String sku, String name, int quantity, 
                          BigDecimal unitPrice, BigDecimal discountAmount, 
                          BigDecimal subtotal) {
            this.sku = sku;
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.discountAmount = discountAmount;
            this.subtotal = subtotal;
        }
        
        // Getters y setters
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { 
            this.discountAmount = discountAmount; 
        }
        
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
    
    /**
     * Clase para encapsular los totales de la venta.
     */
    public static class SaleTotals {
        private final BigDecimal subtotal;
        private final BigDecimal discountTotal;
        private final BigDecimal taxTotal;
        private final BigDecimal grandTotal;
        
        public SaleTotals(BigDecimal subtotal, BigDecimal discountTotal, 
                         BigDecimal taxTotal, BigDecimal grandTotal) {
            this.subtotal = subtotal;
            this.discountTotal = discountTotal;
            this.taxTotal = taxTotal;
            this.grandTotal = grandTotal;
        }
        
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscountTotal() { return discountTotal; }
        public BigDecimal getTaxTotal() { return taxTotal; }
        public BigDecimal getGrandTotal() { return grandTotal; }
    }
}
