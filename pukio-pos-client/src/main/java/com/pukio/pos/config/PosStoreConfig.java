package com.pukio.pos.config;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.model.ProductRecord;
import com.pukio.common.model.SaleRecord;
import com.pukio.common.store.BTreeIndexedFileStore;
import com.pukio.common.store.IndexedFileStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Spring configuration for IndexedFileStore beans in pukio-pos-client.
 * All file paths injected from application.properties via ${VAR} placeholders. (TASK-E1-22)
 */
@Configuration
public class PosStoreConfig {

    @Value("${pukio.files.products}")
    private String productsFilePath;

    @Value("${pukio.files.inventory}")
    private String inventoryFilePath;

    @Value("${pukio.files.sales}")
    private String salesFilePath;

    @Bean
    public IndexedFileStore<String, ProductRecord> productStore() throws IOException {
        return new BTreeIndexedFileStore<>(
                Paths.get(productsFilePath + ".dat"),
                Paths.get(productsFilePath + ".idx"),
                ProductRecord::isDeleted,
                ProductRecord::setDeleted
        );
    }

    @Bean
    public IndexedFileStore<String, InventoryRecord> inventoryStore() throws IOException {
        return new BTreeIndexedFileStore<>(
                Paths.get(inventoryFilePath + ".dat"),
                Paths.get(inventoryFilePath + ".idx"),
                record -> false,
                (record, flag) -> {}
        );
    }

    @Bean
    public IndexedFileStore<String, SaleRecord> saleStore() throws IOException {
        return new BTreeIndexedFileStore<>(
                Paths.get(salesFilePath + ".dat"),
                Paths.get(salesFilePath + ".idx"),
                record -> false,
                (record, flag) -> {}
        );
    }
}
