package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class InventoryMovementReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalInQuantity;
    private BigDecimal totalOutQuantity;
    private BigDecimal netQuantityChange;

    private List<InventoryTransactionResponse> movements;
}
