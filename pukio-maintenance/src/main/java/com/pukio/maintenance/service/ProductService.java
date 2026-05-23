package com.pukio.maintenance.service;

import com.pukio.common.model.ProductRecord;
import com.pukio.common.store.IndexedFileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for product catalog management.
 * Uses IndexedFileStore for O(log n) persistence. (REQ 1.1)
 */
@Slf4j
@Service
public class ProductService {

    private final IndexedFileStore<String, ProductRecord> productStore;

    public ProductService(IndexedFileStore<String, ProductRecord> productStore) {
        this.productStore = productStore;
    }

    /**
     * Create a new product. Validates SKU uniqueness before inserting. (REQ 1.1, TASK-E1-20)
     */
    public void createProduct(String sku, String name, BigDecimal price,
                               String category, String description) throws IOException {
        Optional<ProductRecord> existing = productStore.findByKey(sku);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Product with SKU already exists: " + sku);
        }

        ProductRecord record = ProductRecord.builder()
                .sku(sku)
                .name(name)
                .price(price)
                .category(category)
                .description(description)
                .deleted(false)
                .build();

        productStore.insert(sku, record);
        log.info("Created product: sku={}, name={}", sku, name);
    }

    /**
     * Update an existing product's fields.
     */
    public void updateProduct(String sku, String name, BigDecimal price,
                               String category, String description) throws IOException {
        productStore.findByKey(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));

        ProductRecord updated = ProductRecord.builder()
                .sku(sku)
                .name(name)
                .price(price)
                .category(category)
                .description(description)
                .deleted(false)
                .build();

        productStore.update(sku, updated);
        log.info("Updated product: sku={}", sku);
    }

    /**
     * Soft-delete a product by SKU.
     */
    public void deleteProduct(String sku) throws IOException {
        productStore.findByKey(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));

        productStore.delete(sku);
        log.info("Deleted product: sku={}", sku);
    }

    /**
     * Find a product by SKU. Returns empty if not found or deleted.
     */
    public Optional<ProductRecord> findBySku(String sku) throws IOException {
        return productStore.findByKey(sku);
    }

    /**
     * List all non-deleted products.
     */
    public List<ProductRecord> listAll() throws IOException {
        return productStore.readAll();
    }
}
