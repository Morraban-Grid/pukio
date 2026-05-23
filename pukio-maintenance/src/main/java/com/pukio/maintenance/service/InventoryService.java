package com.pukio.maintenance.service;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.store.IndexedFileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for inventory management.
 * Handles stock updates and outOfStock flag logic. (REQ 1.2)
 */
@Slf4j
@Service
public class InventoryService {

    private final IndexedFileStore<String, InventoryRecord> inventoryStore;

    public InventoryService(IndexedFileStore<String, InventoryRecord> inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    /**
     * Set the stock quantity for a SKU.
     * Creates the record if it does not exist.
     * Automatically flags outOfStock when quantity == 0. (REQ 1.2, TASK-E1-21)
     */
    public void updateStock(String sku, int quantity) throws IOException {
        Optional<InventoryRecord> existing = inventoryStore.findByKey(sku);

        InventoryRecord record = InventoryRecord.builder()
                .sku(sku)
                .quantity(quantity)
                .outOfStock(quantity == 0)
                .lastUpdated(LocalDateTime.now())
                .build();

        if (existing.isPresent()) {
            inventoryStore.update(sku, record);
        } else {
            inventoryStore.insert(sku, record);
        }

        log.info("Updated stock: sku={}, quantity={}, outOfStock={}", sku, quantity, quantity == 0);
    }

    /**
     * Decrement stock by the given amount.
     * Throws exception if insufficient stock.
     * Automatically flags outOfStock when quantity reaches 0. (TASK-E1-21)
     */
    public void decrementStock(String sku, int amount) throws IOException {
        InventoryRecord record = inventoryStore.findByKey(sku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));

        if (record.getQuantity() < amount) {
            throw new IllegalStateException("Insufficient stock for SKU: " + sku +
                    ". Available: " + record.getQuantity() + ", requested: " + amount);
        }

        int newQuantity = record.getQuantity() - amount;

        InventoryRecord updated = InventoryRecord.builder()
                .sku(sku)
                .quantity(newQuantity)
                .outOfStock(newQuantity == 0)
                .lastUpdated(LocalDateTime.now())
                .build();

        inventoryStore.update(sku, updated);
        log.info("Decremented stock: sku={}, remaining={}, outOfStock={}", sku, newQuantity, newQuantity == 0);
    }

    /**
     * Manually flag a SKU as out of stock.
     */
    public void flagOutOfStock(String sku) throws IOException {
        InventoryRecord record = inventoryStore.findByKey(sku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));

        InventoryRecord updated = InventoryRecord.builder()
                .sku(sku)
                .quantity(record.getQuantity())
                .outOfStock(true)
                .lastUpdated(LocalDateTime.now())
                .build();

        inventoryStore.update(sku, updated);
        log.info("Flagged outOfStock: sku={}", sku);
    }

    /**
     * Get current inventory record for a SKU.
     */
    public Optional<InventoryRecord> getStock(String sku) throws IOException {
        return inventoryStore.findByKey(sku);
    }
}
