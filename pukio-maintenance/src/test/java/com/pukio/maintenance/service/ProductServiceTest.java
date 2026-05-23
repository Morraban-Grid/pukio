package com.pukio.maintenance.service;

import com.pukio.common.model.ProductRecord;
import com.pukio.common.store.IndexedFileStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private IndexedFileStore<String, ProductRecord> productStore;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productStore);
    }

    @Test
    void createProduct_newSku_shouldInsert() throws IOException {
        when(productStore.findByKey("SKU-001")).thenReturn(Optional.empty());

        productService.createProduct("SKU-001", "Arroz", new BigDecimal("5.50"), "Abarrotes", "Arroz blanco");

        verify(productStore).insert(eq("SKU-001"), any(ProductRecord.class));
    }

    @Test
    void createProduct_duplicateSku_shouldThrow() throws IOException {
        ProductRecord existing = ProductRecord.builder()
                .sku("SKU-001")
                .deleted(false)
                .build();
        when(productStore.findByKey("SKU-001")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct("SKU-001", "Arroz", BigDecimal.ONE, "Cat", "Desc"));
    }

    @Test
    void updateProduct_existingSku_shouldUpdate() throws IOException {
        ProductRecord existing = ProductRecord.builder()
                .sku("SKU-002")
                .deleted(false)
                .build();
        when(productStore.findByKey("SKU-002")).thenReturn(Optional.of(existing));

        productService.updateProduct("SKU-002", "Azucar", new BigDecimal("3.00"), "Abarrotes", "Azucar blanca");

        verify(productStore).update(eq("SKU-002"), any(ProductRecord.class));
    }

    @Test
    void deleteProduct_existingSku_shouldDelete() throws IOException {
        ProductRecord existing = ProductRecord.builder()
                .sku("SKU-003")
                .deleted(false)
                .build();
        when(productStore.findByKey("SKU-003")).thenReturn(Optional.of(existing));

        productService.deleteProduct("SKU-003");

        verify(productStore).delete("SKU-003");
    }

    @Test
    void listAll_shouldReturnAllProducts() throws IOException {
        List<ProductRecord> products = List.of(
                ProductRecord.builder().sku("SKU-004").name("Leche").deleted(false).build(),
                ProductRecord.builder().sku("SKU-005").name("Pan").deleted(false).build()
        );
        when(productStore.readAll()).thenReturn(products);

        List<ProductRecord> result = productService.listAll();

        assertEquals(2, result.size());
    }
}
