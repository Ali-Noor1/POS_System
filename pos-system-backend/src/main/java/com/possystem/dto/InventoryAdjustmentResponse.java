package com.possystem.dto;

import com.possystem.entity.InventoryTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryAdjustmentResponse {

    private Long transactionId;

    private Long productId;
    private String productName;

    private InventoryTransactionType transactionType;

    private BigDecimal quantityChange;
    private BigDecimal stockBefore;
    private BigDecimal stockAfter;

    private String note;

    private String createdByUsername;
    private LocalDateTime createdAt;
}