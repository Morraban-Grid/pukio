package com.pukio.appserver.dataaccess;

import com.pukio.appserver.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Inventory entity.
 * 
 * @author Pukio Team
 * @since Entregable 2
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    /**
     * Find inventory record by product SKU and store ID.
     * 
     * @param sku the product SKU
     * @param storeId the store ID
     * @return Optional containing the inventory record if found
     */
    Optional<Inventory> findByProduct_SkuAndStore_StoreId(String sku, String storeId);
    
    /**
     * Find inventory record by SKU and store ID with pessimistic write lock (SELECT FOR UPDATE).
     * REQ 2.3: THE Application_Server SHALL lock inventory record during sale transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.sku = :sku AND i.store.storeId = :storeId")
    Optional<Inventory> findBySkuAndStoreIdForUpdate(@Param("sku") String sku, @Param("storeId") String storeId);
    
    /**
     * Find all inventory records for a specific store.
     * 
     * @param storeId the store ID
     * @return List of inventory records for the store
     */
    List<Inventory> findByStore_StoreId(String storeId);
}
