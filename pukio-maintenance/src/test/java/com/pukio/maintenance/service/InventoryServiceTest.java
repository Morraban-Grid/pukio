package com.pukio.maintenance.service;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.store.IndexedFileStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private IndexedFileStore<String, InventoryRecord> inventoryStore;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryStore);
    }

    @Test
    void updateStock_newSku_shouldInsert() throws IOException {
        when(inventoryStore.findByKey("SKU-001")).thenReturn(Optional.empty());

        inventoryService.updateStock("SKU-001", 100);

        verify(inventoryStore).insert(eq("SKU-001"), any(InventoryRecord.class));
    }

    @Test
    void updateStock_existingSku_shouldUpdate() throws IOException {
        InventoryRecord existing = buildRecord("SKU-002", 50, false);
        when(inventoryStore.findByKey("SKU-002")).thenReturn(Optional.of(existing));

        inventoryService.updateStock("SKU-002", 80);

        verify(inventoryStore).update(eq("SKU-002"), any(InventoryRecord.class));
    }

    @Test
    void updateStock_zeroQuantity_shouldFlagOutOfStock() throws IOException {
        when(inventoryStore.findByKey("SKU-003")).thenReturn(Optional.empty());

        inventoryService.updateStock("SKU-003", 0);

        verify(inventoryStore).insert(eq("SKU-003"),
                argThat(r -> r.isOutOfStock() && r.getQuantity() == 0));
    }

    @Test
    void decrementStock_sufficient_shouldDecrement() throws IOException {
        InventoryRecord existing = buildRecord("SKU-004", 10, false);
        when(inventoryStore.findByKey("SKU-004")).thenReturn(Optional.of(existing));

        inventoryService.decrementStock("SKU-004", 3);

        verify(inventoryStore).update(eq("SKU-004"),
                argThat(r -> r.getQuantity() == 7 && !r.isOutOfStock()));
    }

    @Test
    void decrementStock_toZero_shouldFlagOutOfStock() throws IOException {
        InventoryRecord existing = buildRecord("SKU-005", 5, false);
        when(inventoryStore.findByKey("SKU-005")).thenReturn(Optional.of(existing));

        inventoryService.decrementStock("SKU-005", 5);

        verify(inventoryStore).update(eq("SKU-005"),
                argThat(r -> r.getQuantity() == 0 && r.isOutOfStock()));
    }

    @Test
    void decrementStock_insufficient_shouldThrow() throws IOException {
        InventoryRecord existing = buildRecord("SKU-006", 2, false);
        when(inventoryStore.findByKey("SKU-006")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> inventoryService.decrementStock("SKU-006", 5));
    }

    @Test
    void flagOutOfStock_shouldSetFlag() throws IOException {
        InventoryRecord existing = buildRecord("SKU-007", 10, false);
        when(inventoryStore.findByKey("SKU-007")).thenReturn(Optional.of(existing));

        inventoryService.flagOutOfStock("SKU-007");

        verify(inventoryStore).update(eq("SKU-007"),
                argThat(InventoryRecord::isOutOfStock));
    }

    private InventoryRecord buildRecord(String sku, int quantity, boolean outOfStock) {
        return InventoryRecord.builder()
                .sku(sku)
                .quantity(quantity)
                .outOfStock(outOfStock)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
