package com.pukio.appserver.business;

import com.pukio.appserver.dataaccess.PromotionRepository;
import com.pukio.appserver.domain.Promotion;
import com.pukio.appserver.domain.PromotionType;
import com.pukio.appserver.domain.SaleItem;
import com.pukio.appserver.dto.PromotionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pukio.appserver.domain.PromotionType.*;

/**
 * Servicio de negocio para evaluación y aplicación de promociones.
 * Requirement 2.4, 5.4: Aplicación de Promociones Automáticas.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;

    /**
     * Evalúa todas las promociones activas y aplica la más beneficiosa por cada ítem.
     * REQ 2.4: WHEN a sale is processed, THE Application_Server SHALL evaluate all active promotions.
     * REQ 2.4: WHERE multiple promotions apply, THE Application_Server SHALL apply the most beneficial discount.
     */
    public Map<SaleItem, BigDecimal> evaluatePromotions(List<SaleItem> saleItems, BigDecimal subtotal, String storeId) {
        log.debug("Evaluating promotions for {} items with subtotal: {}", saleItems.size(), subtotal);

        // Obtener todas las promociones activas
        LocalDate now = LocalDate.now();
        List<Promotion> activePromotions = promotionRepository.findByActiveAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                true, now, now);

        log.debug("Found {} active promotions", activePromotions.size());

        Map<SaleItem, BigDecimal> discounts = new HashMap<>();

        for (SaleItem item : saleItems) {
            BigDecimal bestDiscount = BigDecimal.ZERO;
            Promotion bestPromo = null;

            for (Promotion promo : activePromotions) {
                // Validar monto mínimo de compra
                if (promo.getMinPurchase() != null && subtotal.compareTo(promo.getMinPurchase()) < 0) {
                    continue;
                }

                // Validar scope (alcance)
                if (!isPromotionApplicable(promo, item, storeId)) {
                    continue;
                }

                // Calcular descuento según tipo
                BigDecimal discount = calculateDiscount(promo, item);

                // Quedarse con la más beneficiosa
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    bestPromo = promo;
                }
            }

            if (bestDiscount.compareTo(BigDecimal.ZERO) > 0) {
                discounts.put(item, bestDiscount);
                log.debug("Applied promotion '{}' to item SKU: {} - discount: {}", 
                        bestPromo != null ? bestPromo.getName() : "unknown", 
                        item.getSku(), bestDiscount);
            }
        }

        return discounts;
    }

    /**
     * Verifica si una promoción es aplicable a un ítem según su scope.
     */
    private boolean isPromotionApplicable(Promotion promo, SaleItem item, String storeId) {
        String scope = promo.getScope();
        
        if (scope == null || scope.equalsIgnoreCase("all")) {
            return true;
        }

        // Scope por SKU específico
        if (scope.equalsIgnoreCase(item.getSku())) {
            return true;
        }

        // Scope por categoría (necesitaríamos cargar Product desde item.getSku())
        // Por simplicidad, asumimos que el scope es "all" o SKU específico

        return false;
    }

    /**
     * Sobrecarga simplificada: evalúa promociones para un ítem individual.
     * Usado por SaleService durante procesamiento línea a línea.
     * REQ 2.4: Aplicación de Promociones Automáticas.
     */
    public BigDecimal evaluatePromotions(String sku, int quantity, BigDecimal price) {
        log.debug("Evaluating promotions for SKU: {}, quantity: {}, price: {}", sku, quantity, price);

        LocalDate now = LocalDate.now();
        List<Promotion> activePromotions = promotionRepository.findByActiveAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                true, now, now);

        BigDecimal bestDiscount = BigDecimal.ZERO;
        Promotion bestPromo = null;
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));

        for (Promotion promo : activePromotions) {
            // Validar scope (alcance por SKU o "all")
            String scope = promo.getScope();
            if (scope != null && !scope.equalsIgnoreCase("all") && !scope.equalsIgnoreCase(sku)) {
                continue;
            }

            // Calcular descuento según tipo
            BigDecimal discount = calculateSimpleDiscount(promo, quantity, price);

            // Quedarse con la más beneficiosa
            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                bestPromo = promo;
            }
        }

        if (bestDiscount.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Applied promotion '{}' to SKU: {} - discount: {}", 
                    bestPromo != null ? bestPromo.getName() : "unknown", 
                    sku, bestDiscount);
        }

        return bestDiscount;
    }

    /**
     * Calcula el descuento para un ítem sin usar SaleItem (simplificado).
     */
    private BigDecimal calculateSimpleDiscount(Promotion promo, int quantity, BigDecimal price) {
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));

        if (promo.getType() == null) {
            return BigDecimal.ZERO;
        }

        switch (promo.getType()) {
            case "PERCENTAGE":
                BigDecimal percentage = promo.getValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                return lineTotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);

            case "FIXED_AMOUNT":
                BigDecimal fixedDiscount = promo.getValue();
                return lineTotal.compareTo(fixedDiscount) >= 0 ? fixedDiscount : lineTotal;

            case "BUY_X_GET_Y":
                int totalItems = promo.getValue().intValue();
                if (totalItems <= 1) return BigDecimal.ZERO;
                int freeItems = quantity / totalItems;
                return price.multiply(BigDecimal.valueOf(freeItems)).setScale(2, RoundingMode.HALF_UP);

            default:
                log.warn("Unknown promotion type: {}", promo.getType());
                return BigDecimal.ZERO;
        }
    }

    /**
     * Calcula el descuento según el tipo de promoción.
     * Soporta: PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y.
     */
    private BigDecimal calculateDiscount(Promotion promo, SaleItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        if (promo.getType() == null) {
            return BigDecimal.ZERO;
        }

        switch (promo.getType()) {
            case "PERCENTAGE":
                // Descuento porcentual
                BigDecimal percentage = promo.getValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                return lineTotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);

            case "FIXED_AMOUNT":
                // Descuento fijo (aplicado al total del ítem, no por unidad)
                BigDecimal fixedDiscount = promo.getValue();
                return lineTotal.compareTo(fixedDiscount) >= 0 ? fixedDiscount : lineTotal;

            case "BUY_X_GET_Y":
                // Compra X lleva Y gratis (value representa X+Y)
                // Ejemplo: value=3 significa compra 2 lleva 1 gratis
                int totalItems = promo.getValue().intValue();
                if (totalItems <= 1) return BigDecimal.ZERO;
                
                int freeItems = item.getQuantity() / totalItems;
                return item.getUnitPrice().multiply(BigDecimal.valueOf(freeItems)).setScale(2, RoundingMode.HALF_UP);

            default:
                log.warn("Unknown promotion type: {}", promo.getType());
                return BigDecimal.ZERO;
        }
    }

    /**
     * Obtiene todas las promociones activas.
     * REQ 5.4: Listar promociones activas.
     */
    public List<Promotion> getActivePromotions() {
        log.debug("Getting active promotions");
        return promotionRepository.findByActiveTrue();
    }

    /**
     * Crea una nueva promoción.
     * REQ 5.4: Crear nueva promoción.
     */
    @Transactional
    public Promotion createPromotion(PromotionRequest request) {
        log.debug("Creating promotion: {}", request.getName());

        PromotionType promotionType = parsePromotionType(request.getType());
        
        Promotion promotion = new Promotion();
        promotion.setName(request.getName());
        promotion.setType(promotionType.name());
        promotion.setValue(request.getValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setMinPurchase(request.getMinimumPurchase());
        promotion.setScope(request.getApplicableSkus());
        promotion.setActive(true);
        promotion.setCreatedAt(LocalDateTime.now());

        return promotionRepository.save(promotion);
    }

    /**
     * Actualiza una promoción existente.
     * REQ 5.4: Actualizar promoción.
     */
    @Transactional
    public Promotion updatePromotion(Long id, PromotionRequest request) {
        log.debug("Updating promotion: {}", id);

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + id));

        PromotionType promotionType = parsePromotionType(request.getType());
        
        promotion.setName(request.getName());
        promotion.setType(promotionType.name());
        promotion.setValue(request.getValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setMinPurchase(request.getMinimumPurchase());
        promotion.setScope(request.getApplicableSkus());
        promotion.setUpdatedAt(LocalDateTime.now());

        return promotionRepository.save(promotion);
    }

    /**
     * Convierte String a PromotionType enum.
     */
    private PromotionType parsePromotionType(String typeStr) {
        if (typeStr == null) {
            return PromotionType.PERCENTAGE;
        }
        
        switch (typeStr.toLowerCase()) {
            case "percentage":
                return PromotionType.PERCENTAGE;
            case "fixed_amount":
                return PromotionType.FIXED_AMOUNT;
            case "buy_x_get_y":
                return PromotionType.BUY_X_GET_Y;
            default:
                log.warn("Unknown promotion type: {}, defaulting to PERCENTAGE", typeStr);
                return PromotionType.PERCENTAGE;
        }
    }

    /**
     * Desactiva una promoción.
     * REQ 5.4: Desactivar/eliminar promoción.
     */
    @Transactional
    public void deactivatePromotion(Long id) {
        log.debug("Deactivating promotion: {}", id);

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + id));

        promotion.setActive(false);
        promotion.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }
}
