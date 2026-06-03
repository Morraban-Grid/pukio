package com.pukio.appserver.business;

import com.pukio.appserver.dataaccess.AuditLogRepository;
import com.pukio.appserver.dataaccess.InventoryRepository;
import com.pukio.appserver.domain.AuditLog;
import com.pukio.appserver.domain.Inventory;
import com.pukio.appserver.business.exception.InsufficientStockException;
import com.pukio.appserver.business.exception.ProductNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de negocio para gestión de inventario.
 * Requirement 2.3, 5.2: Control de Inventario Multi-Tienda con verificación de stock.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Verifica si hay stock suficiente para un producto en una tienda.
     * REQ 2.3: WHEN a sale is processed, THE Application_Server SHALL query current stock from Data_Server.
     */
    @Transactional(readOnly = true)
    public int checkStock(String sku, String storeId) {
        log.debug("Checking stock for SKU: {} in store: {}", sku, storeId);
        
        Inventory inventory = inventoryRepository.findByProduct_SkuAndStore_StoreId(sku, storeId)
                .orElse(null);
        
        return inventory != null ? inventory.getQuantity() : 0;
    }

    /**
     * Verifica stock y lanza excepción si es insuficiente.
     * Usado por SaleService durante procesamiento de venta.
     */
    public void checkStock(String sku, String storeId, int requiredQuantity) {
        int available = checkStock(sku, storeId);
        if (available < requiredQuantity) {
            throw new com.pukio.appserver.exception.InsufficientStockException(sku, available, requiredQuantity);
        }
    }

    /**
     * Obtiene todos los registros de inventario de una tienda.
     */
    @Transactional(readOnly = true)
    public java.util.List<Inventory> getInventoryByStore(String storeId) {
        log.debug("Getting inventory for store: {}", storeId);
        return inventoryRepository.findByStore_StoreId(storeId);
    }

    /**
     * Decrementa el stock con lock pesimista para prevenir sobreventa.
     * REQ 2.3: THE Application_Server SHALL lock inventory record during sale transaction.
     * REQ 5.2: WHEN a sale is completed, THE System SHALL decrement inventory for the corresponding store.
     */
    public void decrementStock(String sku, String storeId, int quantity, String userId, String ipAddress) {
        log.debug("Decrementing stock for SKU: {} in store: {} by quantity: {}", sku, storeId, quantity);

        // Obtener el inventario con lock pesimista (SELECT FOR UPDATE)
        Inventory inventory = inventoryRepository.findBySkuAndStoreIdForUpdate(sku, storeId)
                .orElseThrow(() -> new ProductNotFoundException("Inventory not found for SKU: " + sku + " in store: " + storeId));

        Inventory beforeState = cloneInventory(inventory);

        // Verificar stock suficiente
        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for SKU %s in store %s. Available: %d, Required: %d",
                            sku, storeId, inventory.getQuantity(), quantity)
            );
        }

        // Decrementar stock
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setLastUpdated(LocalDateTime.now());
        inventory.setLastUpdatedBy(userId);

        Inventory saved = inventoryRepository.save(inventory);

        // Registrar en audit log
        registerAuditLog(userId, "DECREMENT_STOCK", "Inventory", 
                inventory.getId().toString(), beforeState, saved, ipAddress);

        log.info("Stock decremented: SKU={}, store={}, newQuantity={}", sku, storeId, saved.getQuantity());
    }

    /**
     * Ajusta el inventario manualmente (delta positivo o negativo) con motivo.
     * REQ 5.2: THE System SHALL allow manual inventory adjustments with reason code.
     */
    public Inventory adjustInventory(String sku, String storeId, int delta, String reason, 
                                    String userId, String ipAddress) {
        log.debug("Adjusting inventory for SKU: {} in store: {} by delta: {} with reason: {}", 
                sku, storeId, delta, reason);

        Inventory inventory = inventoryRepository.findByProduct_SkuAndStore_StoreId(sku, storeId)
                .orElseThrow(() -> new ProductNotFoundException("Inventory not found for SKU: " + sku + " in store: " + storeId));

        Inventory beforeState = cloneInventory(inventory);

        int newQuantity = inventory.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Adjustment would result in negative stock");
        }

        inventory.setQuantity(newQuantity);
        inventory.setLastUpdated(LocalDateTime.now());
        inventory.setLastUpdatedBy(userId);

        Inventory saved = inventoryRepository.save(inventory);

        // Registrar en audit log con el motivo
        String operation = delta > 0 ? "ADJUST_STOCK_UP" : "ADJUST_STOCK_DOWN";
        registerAuditLog(userId, operation, "Inventory", 
                inventory.getId().toString(), beforeState, saved, ipAddress, reason);

        log.info("Inventory adjusted: SKU={}, store={}, delta={}, newQuantity={}, reason={}", 
                sku, storeId, delta, saved.getQuantity(), reason);

        return saved;
    }

    /**
     * Transfiere stock entre dos tiendas.
     * REQ 5.2: THE System SHALL support inventory transfers between stores.
     */
    public void transferStock(String sku, String fromStoreId, String toStoreId, int quantity, 
                             String userId, String ipAddress) {
        log.debug("Transferring stock for SKU: {} from store: {} to store: {} quantity: {}", 
                sku, fromStoreId, toStoreId, quantity);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }

        // Decrementar de origen con lock
        Inventory fromInventory = inventoryRepository.findBySkuAndStoreIdForUpdate(sku, fromStoreId)
                .orElseThrow(() -> new ProductNotFoundException("Inventory not found for SKU: " + sku + " in store: " + fromStoreId));

        if (fromInventory.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock in source store %s. Available: %d, Required: %d",
                            fromStoreId, fromInventory.getQuantity(), quantity)
            );
        }

        Inventory fromBefore = cloneInventory(fromInventory);
        fromInventory.setQuantity(fromInventory.getQuantity() - quantity);
        fromInventory.setLastUpdated(LocalDateTime.now());
        fromInventory.setLastUpdatedBy(userId);
        Inventory fromSaved = inventoryRepository.save(fromInventory);

        // Incrementar en destino con lock
        Inventory toInventory = inventoryRepository.findBySkuAndStoreIdForUpdate(sku, toStoreId)
                .orElseThrow(() -> new ProductNotFoundException("Inventory not found for SKU: " + sku + " in store: " + toStoreId));

        Inventory toBefore = cloneInventory(toInventory);
        toInventory.setQuantity(toInventory.getQuantity() + quantity);
        toInventory.setLastUpdated(LocalDateTime.now());
        toInventory.setLastUpdatedBy(userId);
        Inventory toSaved = inventoryRepository.save(toInventory);

        // Registrar ambas operaciones en audit log
        String transferMsg = String.format("Transfer %d units from %s to %s", quantity, fromStoreId, toStoreId);
        registerAuditLog(userId, "TRANSFER_STOCK_OUT", "Inventory", 
                fromInventory.getId().toString(), fromBefore, fromSaved, ipAddress, transferMsg);
        registerAuditLog(userId, "TRANSFER_STOCK_IN", "Inventory", 
                toInventory.getId().toString(), toBefore, toSaved, ipAddress, transferMsg);

        log.info("Stock transferred successfully: SKU={}, from={} (new={}), to={} (new={})", 
                sku, fromStoreId, fromSaved.getQuantity(), toStoreId, toSaved.getQuantity());
    }

    // ==================== HELPER METHODS ====================

    private void registerAuditLog(String userId, String operation, String entity, String entityId,
                                   Object beforeValue, Object afterValue, String ipAddress) {
        registerAuditLog(userId, operation, entity, entityId, beforeValue, afterValue, ipAddress, null);
    }

    private void registerAuditLog(String userId, String operation, String entity, String entityId,
                                   Object beforeValue, Object afterValue, String ipAddress, String additionalInfo) {
        try {
            String afterJson = afterValue != null ? objectMapper.writeValueAsString(afterValue) : null;
            if (additionalInfo != null && afterJson != null) {
                afterJson = String.format("{\"data\":%s,\"info\":\"%s\"}", afterJson, additionalInfo);
            }

            String details = "";
            if (beforeValue != null) {
                details += "Before: " + objectMapper.writeValueAsString(beforeValue) + " ";
            }
            if (afterJson != null) {
                details += "After: " + afterJson;
            }

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(operation)
                    .entityType(entity)
                    .entityId(entityId)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to register audit log for {}::{} on entity {}", operation, entityId, entity, e);
        }
    }

    private Inventory cloneInventory(Inventory inventory) {
        return Inventory.builder()
                .id(inventory.getId())
                .product(inventory.getProduct())
                .store(inventory.getStore())
                .quantity(inventory.getQuantity())
                .reorderPoint(inventory.getReorderPoint())
                .lastUpdated(inventory.getLastUpdated())
                .lastUpdatedBy(inventory.getLastUpdatedBy())
                .build();
    }
}
