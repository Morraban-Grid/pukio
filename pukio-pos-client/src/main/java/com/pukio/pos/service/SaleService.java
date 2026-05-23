package com.pukio.pos.service;

import com.pukio.common.model.*;
import com.pukio.common.store.IndexedFileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for processing sales transactions. (REQ 1.6)
 * Coordinates product lookup, stock verification, total calculation,
 * sale recording, and inventory decrement.
 */
@Slf4j
@Service
public class SaleService {

    private final IndexedFileStore<String, ProductRecord> productStore;
    private final IndexedFileStore<String, InventoryRecord> inventoryStore;
    private final IndexedFileStore<String, SaleRecord> saleStore;

    public SaleService(IndexedFileStore<String, ProductRecord> productStore,
                       IndexedFileStore<String, InventoryRecord> inventoryStore,
                       IndexedFileStore<String, SaleRecord> saleStore) {
        this.productStore = productStore;
        this.inventoryStore = inventoryStore;
        this.saleStore = saleStore;
    }

    // Current cart items accumulated before finalizing the sale
    private final List<LineItem> currentCart = new ArrayList<>();

    /**
     * Add one item to the current cart.
     * Validates product exists and stock is sufficient. (REQ 1.6)
     */
    public LineItem addToCart(String sku, int quantity) throws IOException {
        // 1. Read price from product store using SKU index
        ProductRecord product = productStore.findByKey(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));

        // 2. Verify sufficient stock in inventory store
        InventoryRecord inventory = inventoryStore.findByKey(sku)
                .orElseThrow(() -> new IllegalArgumentException("No inventory record found for SKU: " + sku));

        if (inventory.isOutOfStock()) {
            throw new IllegalStateException("Product is out of stock: " + sku);
        }

        if (inventory.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for SKU: " + sku +
                    ". Available: " + inventory.getQuantity() +
                    ", requested: " + quantity);
        }

        // 3. Calculate subtotal for this line item
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        LineItem item = LineItem.builder()
                .sku(sku)
                .productName(product.getName())
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .subtotal(subtotal)
                .build();

        currentCart.add(item);
        log.debug("Added to cart: sku={}, qty={}, subtotal={}", sku, quantity, subtotal);
        return item;
    }

    /**
     * Finalize the current cart as a sale transaction. (REQ 1.6)
     * Calculates total, records payment, writes SaleRecord,
     * and decrements inventory for each item.
     */
    public SaleRecord processSale(PaymentMethod paymentMethod) throws IOException {
        if (currentCart.isEmpty()) {
            throw new IllegalStateException("Cannot process an empty sale.");
        }

        // 4. Calculate total of all items
        BigDecimal total = currentCart.stream()
                .map(LineItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Build and write SaleRecord
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SaleRecord sale = SaleRecord.builder()
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .items(new ArrayList<>(currentCart))
                .total(total)
                .paymentMethod(paymentMethod)
                .build();

        saleStore.insert(transactionId, sale);
        log.info("Sale recorded: transactionId={}, total={}, method={}",
                transactionId, total, paymentMethod);

        // 6. Decrement inventory for each item in the cart
        for (LineItem item : currentCart) {
            InventoryRecord inv = inventoryStore.findByKey(item.getSku())
                    .orElseThrow(() -> new IllegalStateException("Inventory disappeared for SKU: " + item.getSku()));

            int newQty = inv.getQuantity() - item.getQuantity();
            InventoryRecord updated = InventoryRecord.builder()
                    .sku(item.getSku())
                    .quantity(newQty)
                    .outOfStock(newQty == 0)
                    .lastUpdated(LocalDateTime.now())
                    .build();

            inventoryStore.update(item.getSku(), updated);
            log.debug("Decremented inventory: sku={}, remaining={}", item.getSku(), newQty);
        }

        clearCart();
        return sale;
    }

    /**
     * Cancel the current cart without processing any sale.
     */
    public void cancelSale() {
        currentCart.clear();
        log.info("Sale cancelled, cart cleared.");
    }

    /**
     * Get current cart items (read-only view).
     */
    public List<LineItem> getCurrentCart() {
        return List.copyOf(currentCart);
    }

    /**
     * Clear the cart after a successful sale.
     */
    private void clearCart() {
        currentCart.clear();
    }
}
