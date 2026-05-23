package com.pukio.common.store;

import com.pukio.common.model.ProductRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BTreeIndexedFileStoreTest {

    @TempDir
    Path tempDir;

    private BTreeIndexedFileStore<String, ProductRecord> store;

    @BeforeEach
    void setUp() throws IOException {
        store = new BTreeIndexedFileStore<>(
                tempDir.resolve("products.dat"),
                tempDir.resolve("products.idx"),
                ProductRecord::isDeleted,
                ProductRecord::setDeleted
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        store.close();
    }

    // --- insert ---

    @Test
    void insert_shouldPersistRecord() throws IOException {
        ProductRecord product = buildProduct("SKU-001", "Arroz", new BigDecimal("5.50"));
        store.insert("SKU-001", product);

        Optional<ProductRecord> result = store.findByKey("SKU-001");
        assertTrue(result.isPresent());
        assertEquals("Arroz", result.get().getName());
        assertEquals(new BigDecimal("5.50"), result.get().getPrice());
    }

    @Test
    void insert_duplicateKey_shouldThrowException() throws IOException {
        ProductRecord product = buildProduct("SKU-002", "Azucar", new BigDecimal("3.00"));
        store.insert("SKU-002", product);

        assertThrows(IllegalArgumentException.class,
                () -> store.insert("SKU-002", product));
    }

    // --- update ---

    @Test
    void update_shouldChangeRecord() throws IOException {
        store.insert("SKU-003", buildProduct("SKU-003", "Sal", new BigDecimal("1.00")));
        store.update("SKU-003", buildProduct("SKU-003", "Sal de Mesa", new BigDecimal("1.50")));

        Optional<ProductRecord> result = store.findByKey("SKU-003");
        assertTrue(result.isPresent());
        assertEquals("Sal de Mesa", result.get().getName());
        assertEquals(new BigDecimal("1.50"), result.get().getPrice());
    }

    @Test
    void update_nonExistentKey_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> store.update("SKU-999",
                        buildProduct("SKU-999", "Ghost", BigDecimal.ZERO)));
    }

    // --- delete (soft) ---

    @Test
    void delete_shouldMarkRecordAsDeleted() throws IOException {
        store.insert("SKU-004", buildProduct("SKU-004", "Aceite", new BigDecimal("8.00")));
        store.delete("SKU-004");

        Optional<ProductRecord> result = store.findByKey("SKU-004");
        assertFalse(result.isPresent());
    }

    @Test
    void delete_shouldNotRemovePhysicalRecord() throws IOException {
        store.insert("SKU-005", buildProduct("SKU-005", "Harina", new BigDecimal("4.00")));
        store.delete("SKU-005");

        List<ProductRecord> all = store.readAll();
        assertTrue(all.stream().noneMatch(p -> "SKU-005".equals(p.getSku())));
    }

    // --- readAll ---

    @Test
    void readAll_shouldReturnOnlyNonDeletedRecords() throws IOException {
        store.insert("SKU-006", buildProduct("SKU-006", "Leche", new BigDecimal("3.50")));
        store.insert("SKU-007", buildProduct("SKU-007", "Pan", new BigDecimal("2.00")));
        store.delete("SKU-006");

        List<ProductRecord> all = store.readAll();
        assertEquals(1, all.size());
        assertEquals("SKU-007", all.get(0).getSku());
    }

    // --- findByKey ---

    @Test
    void findByKey_nonExistentKey_shouldReturnEmpty() throws IOException {
        Optional<ProductRecord> result = store.findByKey("SKU-NOT-EXIST");
        assertFalse(result.isPresent());
    }

    // --- concurrency ---

    @Test
    void concurrentReads_shouldNotThrowException() throws Exception {
        store.insert("SKU-010", buildProduct("SKU-010", "Yogurt", new BigDecimal("4.50")));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Optional<ProductRecord> result = store.findByKey("SKU-010");
                    assertTrue(result.isPresent());
                } catch (IOException e) {
                    fail("Concurrent read threw exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    void concurrentInserts_shouldPersistAllRecords() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    String sku = "SKU-CONC-" + idx;
                    store.insert(sku, buildProduct(sku, "Product " + idx,
                            new BigDecimal("1.00")));
                } catch (IOException e) {
                    fail("Concurrent insert threw exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        List<ProductRecord> all = store.readAll();
        assertEquals(threadCount, all.size());
    }

    // --- helper ---

    private ProductRecord buildProduct(String sku, String name, BigDecimal price) {
        return ProductRecord.builder()
                .sku(sku)
                .name(name)
                .price(price)
                .category("Abarrotes")
                .description("Producto de prueba")
                .deleted(false)
                .build();
    }
}
