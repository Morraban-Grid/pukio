package com.pukio.posclient.dto;

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
public class ArqueoResult {
    private Long arqueoId;
    private String status;
    private Map<String, BigDecimal> variancesByMethod;
    private String message;
}
