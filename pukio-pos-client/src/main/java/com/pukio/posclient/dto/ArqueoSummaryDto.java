package com.pukio.posclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArqueoSummaryDto {
    private List<ArqueoExpectedDto> items;
    
    /**
     * Get expected amounts grouped by payment method.
     * @return Map of payment method to expected amount
     */
    public Map<String, BigDecimal> getExpectedByMethod() {
        if (items == null) {
            return Map.of();
        }
        return items.stream()
            .collect(Collectors.toMap(
                ArqueoExpectedDto::getMethod,
                ArqueoExpectedDto::getExpectedAmount
            ));
    }
}
