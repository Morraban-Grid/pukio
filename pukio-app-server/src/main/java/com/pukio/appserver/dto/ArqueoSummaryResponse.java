package com.pukio.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArqueoSummaryResponse {
    private Map<String, BigDecimal> expectedAmounts;
    private String shiftId;
}
