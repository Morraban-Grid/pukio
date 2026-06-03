package com.pukio.appserver.business;

import com.pukio.appserver.dataaccess.AuditLogRepository;
import com.pukio.appserver.dataaccess.ProductRepository;
import com.pukio.appserver.dataaccess.SaleRepository;
import com.pukio.appserver.domain.AuditLog;
import com.pukio.appserver.domain.Product;
import com.pukio.appserver.business.exception.DuplicateSkuException;
import com.pukio.appserver.business.exception.ProductNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de negocio para gestión de productos (CRUD completo).
 * Requirement 5.1: Gestión Completa de Productos.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea un nuevo producto. Valida que el SKU sea único.
     * REQ 5.1: THE System SHALL validate SKU uniqueness before creating product.
     */
    public Product createProduct(Product product, String userId, String ipAddress) {
        log.debug("Creating product with SKU: {}", product.getSku());

        // Validar SKU único
        if (productRepository.existsById(product.getSku())) {
            throw new DuplicateSkuException("Product with SKU " + product.getSku() + " already exists");
        }

        // Establecer timestamps
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        // Registrar en audit log
        registerAuditLog(userId, "CREATE", "Product", product.getSku(), null, savedProduct, ipAddress);

        log.info("Product created successfully: SKU={}", savedProduct.getSku());
        return savedProduct;
    }

    /**
     * Actualiza un producto existente. El SKU no es editable.
     * REQ 5.1: THE System SHALL allow updating product information except SKU.
     */
    public Product updateProduct(String sku, Product updatedProduct, String userId, String ipAddress) {
        log.debug("Updating product with SKU: {}", sku);

        Product existing = productRepository.findById(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + sku));

        // Guardar estado anterior para audit
        Product beforeState = cloneProduct(existing);

        // Actualizar campos (SKU no cambia)
        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setCategory(updatedProduct.getCategory());
        existing.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(existing);

        // Registrar en audit log
        registerAuditLog(userId, "UPDATE", "Product", sku, beforeState, saved, ipAddress);

        log.info("Product updated successfully: SKU={}", sku);
        return saved;
    }

    /**
     * Desactiva o elimina un producto.
     * REQ 5.1: THE System SHALL allow deleting products that have no associated sales.
     * REQ 5.1: IF a product has sales history, THEN THE System SHALL mark it as inactive instead of deleting.
     */
    public void deactivateProduct(String sku, String userId, String ipAddress) {
        log.debug("Deactivating/deleting product with SKU: {}", sku);

        Product product = productRepository.findById(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + sku));

        // Verificar si tiene ventas asociadas
        boolean hasSales = saleRepository.existsBySku(sku);

        if (hasSales) {
            // Tiene ventas: solo marcar como inactivo
            Product beforeState = cloneProduct(product);
            product.setActive(false);
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
            registerAuditLog(userId, "DEACTIVATE", "Product", sku, beforeState, product, ipAddress);
            log.info("Product deactivated (has sales history): SKU={}", sku);
        } else {
            // No tiene ventas: eliminar físicamente
            registerAuditLog(userId, "DELETE", "Product", sku, product, null, ipAddress);
            productRepository.delete(product);
            log.info("Product deleted (no sales history): SKU={}", sku);
        }
    }

    /**
     * Busca un producto por SKU.
     * REQ 5.1: THE System SHALL allow searching products by SKU, name, or category.
     */
    @Transactional(readOnly = true)
    public Product findBySku(String sku) {
        return productRepository.findById(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + sku));
    }

    /**
     * Busca productos con filtros y paginación.
     * REQ 5.1: THE System SHALL display product list with pagination (50 products per page).
     */
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, String category, int page, int size) {
        log.debug("Searching products: query={}, category={}, page={}, size={}", query, category, page, size);

        if (size <= 0 || size > 100) {
            size = 50; // Default y máximo
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        if (query != null && !query.isBlank() && category != null && !category.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(query, category, pageable);
        } else if (query != null && !query.isBlank()) {
            return productRepository.findByNameContainingIgnoreCase(query, pageable);
        } else if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryIgnoreCase(category, pageable);
        } else {
            return productRepository.findAll(pageable);
        }
    }

    // ==================== HELPER METHODS ====================

    private void registerAuditLog(String userId, String operation, String entity, String entityId,
                                   Object beforeValue, Object afterValue, String ipAddress) {
        try {
            String details = "";
            if (beforeValue != null) {
                details += "Before: " + objectMapper.writeValueAsString(beforeValue) + " ";
            }
            if (afterValue != null) {
                details += "After: " + objectMapper.writeValueAsString(afterValue);
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
            // No lanzar excepción: el audit log es importante pero no debe romper la operación principal
        }
    }

    private Product cloneProduct(Product product) {
        Product clone = new Product();
        clone.setSku(product.getSku());
        clone.setName(product.getName());
        clone.setDescription(product.getDescription());
        clone.setPrice(product.getPrice());
        clone.setCategory(product.getCategory());
        clone.setActive(product.getActive());
        clone.setCreatedAt(product.getCreatedAt());
        clone.setUpdatedAt(product.getUpdatedAt());
        return clone;
    }
}
