package com.pukio.update;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.model.ProductRecord;
import com.pukio.update.repository.InventorySyncRepository;
import com.pukio.update.repository.ProductSyncRepository;
import com.pukio.update.service.IndexedFileParser;
import com.pukio.update.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UpdateService using H2 in-memory database. (TASK-E1-47)
 * Uses @ActiveProfiles("test") to load application-test.properties with H2 config.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UpdateServiceIntegrationTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private IndexedFileParser parser;

    @Autowired
    private ProductSyncRepository productRepo;

    @Autowired
    private InventorySyncRepository inventoryRepo;

    @Test
    void syncAll_validData_shouldPersistProductsAndInventory() throws Exception {
        Map<String, byte[]> fileMap = new HashMap<>();
        fileMap.put("products.dat", serializeProduct(
                buildProduct("SKU-001", "Arroz", new BigDecimal("5.50"))));
        fileMap.put("inventory.dat", serializeInventory(
                buildInventory("SKU-001", 100, false)));

        Map<String, Integer> counts = syncService.syncAll(fileMap, "STORE-TEST");

        assertEquals(1, counts.get("products"));
        assertEquals(1, counts.get("inventory"));
    }

    @Test
    void syncAll_emptyFiles_shouldReturnZeroCounts() throws Exception {
        Map<String, byte[]> fileMap = new HashMap<>();
        fileMap.put("products.dat", new byte[0]);
        fileMap.put("inventory.dat", new byte[0]);

        Map<String, Integer> counts = syncService.syncAll(fileMap, "STORE-TEST");

        assertEquals(0, counts.get("products"));
        assertEquals(0, counts.get("inventory"));
    }

    @Test
    void syncAll_upsertSameSku_shouldNotDuplicate() throws Exception {
        Map<String, byte[]> fileMap = new HashMap<>();
        fileMap.put("products.dat", serializeProduct(
                buildProduct("SKU-002", "Azucar", new BigDecimal("3.00"))));
        fileMap.put("inventory.dat", new byte[0]);

        // Sync twice — upsert should not create duplicates
        syncService.syncAll(fileMap, "STORE-TEST");

        // Update the product and sync again
        fileMap.put("products.dat", serializeProduct(
                buildProduct("SKU-002", "Azucar Rubia", new BigDecimal("3.50"))));

        Map<String, Integer> counts = syncService.syncAll(fileMap, "STORE-TEST");

        assertEquals(1, counts.get("products"));
    }

    @Test
    void parseProducts_validBytes_shouldReturnRecords() throws IOException {
        byte[] bytes = serializeProduct(
                buildProduct("SKU-003", "Sal", new BigDecimal("1.00")));

        var products = parser.parseProducts(bytes);

        assertEquals(1, products.size());
        assertEquals("SKU-003", products.get(0).getSku());
    }

    @Test
    void parseInventory_validBytes_shouldReturnRecords() throws IOException {
        byte[] bytes = serializeInventory(
                buildInventory("SKU-004", 50, false));

        var inventory = parser.parseInventory(bytes);

        assertEquals(1, inventory.size());
        assertEquals("SKU-004", inventory.get(0).getSku());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private byte[] serializeProduct(ProductRecord record) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(record);
        }
        return baos.toByteArray();
    }

    private byte[] serializeInventory(InventoryRecord record) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(record);
        }
        return baos.toByteArray();
    }

    private ProductRecord buildProduct(String sku, String name, BigDecimal price) {
        return ProductRecord.builder()
                .sku(sku)
                .name(name)
                .price(price)
                .category("Test")
                .description("Test product")
                .deleted(false)
                .build();
    }

    private InventoryRecord buildInventory(String sku, int qty, boolean outOfStock) {
        return InventoryRecord.builder()
                .sku(sku)
                .quantity(qty)
                .outOfStock(outOfStock)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
