package com.pukio.maintenance.config;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.model.ProductRecord;
import com.pukio.common.store.BTreeIndexedFileStore;
import com.pukio.common.store.IndexedFileStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring configuration for IndexedFileStore beans.
 * File paths are injected from application.properties via ${VAR} placeholders. (TASK-E1-22)
 */
@Configuration
public class StoreConfig {

    @Value("${pukio.files.products}")
    private String productsFilePath;

    @Value("${pukio.files.inventory}")
    private String inventoryFilePath;

    @Bean
    public IndexedFileStore<String, ProductRecord> productStore() throws IOException {
        Path dataFile = Paths.get(productsFilePath + ".dat");
        Path indexFile = Paths.get(productsFilePath + ".idx");
        return new BTreeIndexedFileStore<>(
                dataFile,
                indexFile,
                ProductRecord::isDeleted,
                ProductRecord::setDeleted
        );
    }

    @Bean
    public IndexedFileStore<String, InventoryRecord> inventoryStore() throws IOException {
        Path dataFile = Paths.get(inventoryFilePath + ".dat");
        Path indexFile = Paths.get(inventoryFilePath + ".idx");
        return new BTreeIndexedFileStore<>(
                dataFile,
                indexFile,
                record -> false,
                (record, flag) -> {}
        );
    }
}
