package com.pukio.update.repository;

import com.pukio.common.model.ProductRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository for upserting ProductRecord into the central PostgreSQL database.
 * Uses INSERT ... ON CONFLICT DO UPDATE for idempotent sync. (REQ 1.4, TASK-E1-41)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductSyncRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String UPSERT_SQL = """
        INSERT INTO products (sku, store_id, name, price, category, description, deleted, synced_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
        ON CONFLICT (sku, store_id)
        DO UPDATE SET
            name        = EXCLUDED.name,
            price       = EXCLUDED.price,
            category    = EXCLUDED.category,
            description = EXCLUDED.description,
            deleted     = EXCLUDED.deleted,
            synced_at   = NOW()
        """;

    /**
     * Upsert a single ProductRecord for the given store.
     */
    public void upsert(ProductRecord record, String storeId) {
        jdbcTemplate.update(UPSERT_SQL,
                record.getSku(),
                storeId,
                record.getName(),
                record.getPrice(),
                record.getCategory(),
                record.getDescription(),
                record.isDeleted());

        log.debug("Upserted product: sku={}, storeId={}", record.getSku(), storeId);
    }
}
