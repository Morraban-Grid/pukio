package com.pukio.appserver.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pukio.appserver.dataaccess.*;
import com.pukio.appserver.domain.*;
import com.pukio.appserver.dto.ArqueoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArqueoService {

    private final ArqueoRepository arqueoRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${ARQUEO_VARIANCE_THRESHOLD:50}")
    private BigDecimal varianceThreshold;

    @Transactional
    public Arqueo calcularArqueo(ArqueoRequest request) {
        log.info("Calculating arqueo for store: {}, shift: {}", request.getStoreId(), request.getShiftId());

        // Get all sales for the store and shift
        List<Sale> sales = saleRepository.findByStore_StoreIdAndShiftId(request.getStoreId(), request.getShiftId());

        // Calculate expected amounts grouped by payment method
        Map<String, BigDecimal> expectedAmounts = new HashMap<>();
        for (Sale sale : sales) {
            List<Payment> payments = paymentRepository.findBySale(sale);
            for (Payment payment : payments) {
                expectedAmounts.merge(payment.getPaymentMethod(), payment.getAmount(), BigDecimal::add);
            }
        }

        // Calculate total variance
        BigDecimal totalVariance = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : request.getDeclaredAmounts().entrySet()) {
            String method = entry.getKey();
            BigDecimal declared = entry.getValue();
            BigDecimal expected = expectedAmounts.getOrDefault(method, BigDecimal.ZERO);
            totalVariance = totalVariance.add(declared.subtract(expected).abs());
        }
        // Also account for expected methods not declared
        for (Map.Entry<String, BigDecimal> entry : expectedAmounts.entrySet()) {
            if (!request.getDeclaredAmounts().containsKey(entry.getKey())) {
                totalVariance = totalVariance.add(entry.getValue().abs());
            }
        }

        // Determine status
        String status = totalVariance.compareTo(varianceThreshold) > 0 ? "PENDING_APPROVAL" : "APPROVED";

        // Serialize maps as JSON
        String expectedAmountsJson;
        String declaredAmountsJson;
        try {
            expectedAmountsJson = objectMapper.writeValueAsString(expectedAmounts);
            declaredAmountsJson = objectMapper.writeValueAsString(request.getDeclaredAmounts());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize amounts", e);
        }

        // Save Arqueo
        Arqueo arqueo = new Arqueo();
        arqueo.setStoreId(request.getStoreId());
        arqueo.setShiftId(request.getShiftId());
        arqueo.setExpectedAmounts(expectedAmountsJson);
        arqueo.setDeclaredAmounts(declaredAmountsJson);
        arqueo.setTotalVariance(totalVariance);
        arqueo.setStatus(status);
        arqueo.setCreatedAt(LocalDateTime.now());
        arqueo = arqueoRepository.save(arqueo);

        // Save AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("ARQUEO_SUBMITTED");
        auditLog.setEntityType("Arqueo");
        auditLog.setEntityId(arqueo.getArqueoId().toString());
        auditLog.setDetails("Store: " + request.getStoreId() + ", Shift: " + request.getShiftId()
                + ", Variance: " + totalVariance + ", Status: " + status);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        log.info("Arqueo submitted: id={}, status={}, totalVariance={}", arqueo.getArqueoId(), status, totalVariance);

        return arqueo;
    }

    @Transactional
    public Arqueo approveArqueo(Long id) {
        log.info("Approving arqueo: {}", id);

        Arqueo arqueo = arqueoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arqueo not found: " + id));

        arqueo.setStatus("APPROVED");
        arqueo.setApprovedAt(LocalDateTime.now());

        arqueo = arqueoRepository.save(arqueo);

        log.info("Arqueo approved: {}", id);

        return arqueo;
    }
}
