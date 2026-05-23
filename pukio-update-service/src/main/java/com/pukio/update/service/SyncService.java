package com.pukio.update.service;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.model.ProductRecord;
import com.pukio.update.repository.InventorySyncRepository;
import com.pukio.update.repository.ProductSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full sync pipeline:
 * parse received files → upsert into PostgreSQL → return counts.
 * All DB operations run inside a single @Transactional. (TASK-E1-43, TASK-E1-44)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final IndexedFileParser parser;
    private final ProductSyncRepository productRepo;
    private final InventorySyncRepository inventoryRepo;

    /**
     * Parse and sync all received file bytes in a single transaction.
     * If any operation fails, the entire transaction rolls back. (REQ 1.4)
     *
     * @param fileMap   map of logical filename -> raw bytes
     * @param storeId   identifier of the originating store
     * @return map with counts: "products" and "inventory"
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> syncAll(Map<String, byte[]> fileMap,
                                        String storeId) throws IOException {

        log.info("Starting sync for storeId={} at {}", storeId, LocalDateTime.now());

        // Parse product records from .dat file
        byte[] productsDat = fileMap.getOrDefault("products.dat", new byte[0]);
        List<ProductRecord> products = parser.parseProducts(productsDat);

        // Parse inventory records from .dat file
        byte[] inventoryDat = fileMap.getOrDefault("inventory.dat", new byte[0]);
        List<InventoryRecord> inventoryList = parser.parseInventory(inventoryDat);

        // Upsert all products — inside single transaction
        for (ProductRecord product : products) {
            productRepo.upsert(product, storeId);
        }

        // Upsert all inventory — inside same transaction
        for (InventoryRecord inventory : inventoryList) {
            inventoryRepo.upsert(inventory, storeId);
        }

        log.info("Sync complete for storeId={}: {} products, {} inventory records.",
                storeId, products.size(), inventoryList.size());

        return Map.of(
                "products", products.size(),
                "inventory", inventoryList.size()
        );
    }
}
