package com.pukio.pos.service;

import com.pukio.common.model.*;
import com.pukio.common.store.IndexedFileStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private IndexedFileStore<String, ProductRecord> productStore;

    @Mock
    private IndexedFileStore<String, InventoryRecord> inventoryStore;

    @Mock
    private IndexedFileStore<String, SaleRecord> saleStore;

    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(productStore, inventoryStore, saleStore);
    }

    @Test
    void addToCart_validProduct_shouldAddLineItem() throws IOException {
        when(productStore.findByKey("SKU-001"))
                .thenReturn(Optional.of(buildProduct("SKU-001", "Arroz", new BigDecimal("5.00"))));
        when(inventoryStore.findByKey("SKU-001"))
                .thenReturn(Optional.of(buildInventory("SKU-001", 10, false)));

        LineItem item = saleService.addToCart("SKU-001", 2);

        assertEquals("SKU-001", item.getSku());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("10.00"), item.getSubtotal());
        assertEquals(1, saleService.getCurrentCart().size());
    }

    @Test
    void addToCart_productNotFound_shouldThrow() throws IOException {
        when(productStore.findByKey("SKU-999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> saleService.addToCart("SKU-999", 1));
    }

    @Test
    void addToCart_outOfStock_shouldThrow() throws IOException {
        when(productStore.findByKey("SKU-002"))
                .thenReturn(Optional.of(buildProduct("SKU-002", "Sal", new BigDecimal("1.00"))));
        when(inventoryStore.findByKey("SKU-002"))
                .thenReturn(Optional.of(buildInventory("SKU-002", 0, true)));

        assertThrows(IllegalStateException.class,
                () -> saleService.addToCart("SKU-002", 1));
    }

    @Test
    void addToCart_insufficientStock_shouldThrow() throws IOException {
        when(productStore.findByKey("SKU-003"))
                .thenReturn(Optional.of(buildProduct("SKU-003", "Aceite", new BigDecimal("8.00"))));
        when(inventoryStore.findByKey("SKU-003"))
                .thenReturn(Optional.of(buildInventory("SKU-003", 2, false)));

        assertThrows(IllegalStateException.class,
                () -> saleService.addToCart("SKU-003", 5));
    }

    @Test
    void processSale_validCart_shouldRecordSaleAndDecrementInventory()
            throws IOException {
        when(productStore.findByKey("SKU-004"))
                .thenReturn(Optional.of(buildProduct("SKU-004", "Leche", new BigDecimal("3.50"))));
        when(inventoryStore.findByKey("SKU-004"))
                .thenReturn(Optional.of(buildInventory("SKU-004", 20, false)));

        saleService.addToCart("SKU-004", 3);

        when(inventoryStore.findByKey("SKU-004"))
                .thenReturn(Optional.of(buildInventory("SKU-004", 20, false)));

        SaleRecord sale = saleService.processSale(PaymentMethod.CASH);

        assertNotNull(sale.getTransactionId());
        assertEquals(new BigDecimal("10.50"), sale.getTotal());
        assertEquals(PaymentMethod.CASH, sale.getPaymentMethod());
        assertEquals(1, sale.getItems().size());

        verify(saleStore).insert(eq(sale.getTransactionId()), any(SaleRecord.class));
        verify(inventoryStore).update(eq("SKU-004"), any(InventoryRecord.class));
        assertTrue(saleService.getCurrentCart().isEmpty());
    }

    @Test
    void processSale_emptyCart_shouldThrow() {
        assertThrows(IllegalStateException.class,
                () -> saleService.processSale(PaymentMethod.CARD));
    }

    @Test
    void cancelSale_shouldClearCart() throws IOException {
        when(productStore.findByKey("SKU-005"))
                .thenReturn(Optional.of(buildProduct("SKU-005", "Pan", new BigDecimal("2.00"))));
        when(inventoryStore.findByKey("SKU-005"))
                .thenReturn(Optional.of(buildInventory("SKU-005", 5, false)));

        saleService.addToCart("SKU-005", 1);
        assertFalse(saleService.getCurrentCart().isEmpty());

        saleService.cancelSale();
        assertTrue(saleService.getCurrentCart().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
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
