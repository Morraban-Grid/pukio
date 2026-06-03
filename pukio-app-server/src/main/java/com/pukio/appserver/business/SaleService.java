package com.pukio.appserver.business;

import com.pukio.appserver.dataaccess.*;
import com.pukio.appserver.domain.*;
import com.pukio.appserver.dto.*;
import com.pukio.appserver.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PromotionService promotionService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(rollbackFor = Exception.class)
    public SaleResponse processSale(SaleRequest request) {
        log.info("Processing sale for store: {}, shift: {}", request.getStoreId(), request.getShiftId());

        List<SaleItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : request.getItems()) {
            // Find active product
            Product product = productRepository.findBySkuAndActiveTrue(itemReq.getSku())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.getSku()));

            // Check stock
            inventoryService.checkStock(itemReq.getSku(), request.getStoreId(), itemReq.getQuantity());

            // Evaluate promotions
            BigDecimal discount = promotionService.evaluatePromotions(
                    itemReq.getSku(), itemReq.getQuantity(), product.getPrice());

            // Calculate line subtotal
            BigDecimal lineSubtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);

            itemResponses.add(SaleItemResponse.builder()
                    .sku(itemReq.getSku())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .discount(discount)
                    .subtotal(lineSubtotal)
                    .build());

            subtotal = subtotal.add(lineSubtotal);
            totalDiscount = totalDiscount.add(discount);
        }

        // Calculate IGV and grand total
        BigDecimal igv = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(igv);

        // Validate payments match grand total
        BigDecimal paymentsTotal = request.getPayments().stream()
                .map(PaymentRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (paymentsTotal.compareTo(grandTotal) != 0) {
            throw new BusinessException("Payment total does not match grand total");
        }

        // Generate transaction ID
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Save Sale
        Sale sale = new Sale();
        sale.setTransactionId(transactionId);
        sale.setStoreId(request.getStoreId());
        sale.setShiftId(request.getShiftId());
        sale.setSubtotal(subtotal);
        sale.setIgv(igv);
        sale.setGrandTotal(grandTotal);
        sale.setTotalDiscount(totalDiscount);
        sale.setTransactionDate(LocalDateTime.now());
        sale.setStatus("COMPLETED");
        sale = saleRepository.save(sale);

        // Save SaleItems
        for (SaleItemResponse itemResp : itemResponses) {
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setSku(itemResp.getSku());
            saleItem.setProductName(itemResp.getProductName());
            saleItem.setQuantity(itemResp.getQuantity());
            saleItem.setUnitPrice(itemResp.getUnitPrice());
            saleItem.setDiscount(itemResp.getDiscount());
            saleItem.setSubtotal(itemResp.getSubtotal());
            saleItemRepository.save(saleItem);
        }

        // Save Payments
        for (PaymentRequest payReq : request.getPayments()) {
            Payment payment = new Payment();
            payment.setSale(sale);
            payment.setPaymentMethod(payReq.getPaymentMethod());
            payment.setAmount(payReq.getAmount());
            paymentRepository.save(payment);
        }

        // Decrement inventory for each item
        for (SaleItemRequest itemReq : request.getItems()) {
            inventoryService.decrementStock(itemReq.getSku(), request.getStoreId(), itemReq.getQuantity(), null, null);
        }

        // Save AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("SALE_PROCESSED");
        auditLog.setEntityType("Sale");
        auditLog.setEntityId(sale.getSaleId() != null ? sale.getSaleId().toString() : transactionId);
        auditLog.setDetails(transactionId + " " + grandTotal + " " + request.getStoreId());
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        log.info("Sale processed successfully: transactionId={}, grandTotal={}", transactionId, grandTotal);

        return SaleResponse.builder()
                .transactionId(transactionId)
                .status("COMPLETED")
                .timestamp(sale.getTransactionDate())
                .items(itemResponses)
                .subtotal(subtotal)
                .igv(igv)
                .grandTotal(grandTotal)
                .totalDiscount(totalDiscount)
                .payments(request.getPayments())
                .build();
    }
}
