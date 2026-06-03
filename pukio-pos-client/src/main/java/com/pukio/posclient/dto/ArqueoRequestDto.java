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
public class ArqueoRequestDto {
    private String storeId;
    private String cashierId;
    private String shiftId;
    private Map<String, BigDecimal> declaredAmounts;
}
