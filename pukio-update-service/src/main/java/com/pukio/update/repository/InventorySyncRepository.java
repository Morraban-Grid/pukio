package com.pukio.update.repository;

import com.pukio.common.model.InventoryRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository for upserting InventoryRecord into the central PostgreSQL database.
 * Uses INSERT ... ON CONFLICT DO UPDATE for idempotent sync. (REQ 1.4, TASK-E1-42)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InventorySyncRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String UPSERT_SQL = """
        INSERT INTO inventory (sku, store_id, quantity, out_of_stock, last_updated, synced_at)
        VALUES (?, ?, ?, ?, ?, NOW())
        ON CONFLICT (sku, store_id)
        DO UPDATE SET
            quantity     = EXCLUDED.quantity,
            out_of_stock = EXCLUDED.out_of_stock,
            last_updated = EXCLUDED.last_updated,
            synced_at    = NOW()
        """;

    /**
     * Upsert a single InventoryRecord for the given store.
     */
    public void upsert(InventoryRecord record, String storeId) {
        jdbcTemplate.update(UPSERT_SQL,
                record.getSku(),
                storeId,
                record.getQuantity(),
                record.isOutOfStock(),
                record.getLastUpdated());

        log.debug("Upserted inventory: sku={}, storeId={}, qty={}",
                record.getSku(), storeId, record.getQuantity());
    }
}
