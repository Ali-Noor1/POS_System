package com.possystem.dto;

import com.possystem.entity.InventoryTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryTransactionResponse {

    private Long id;

    private Long productId;
    private String productName;

    private InventoryTransactionType transactionType;

    private BigDecimal quantityChange;
    private BigDecimal stockBefore;
    private BigDecimal stockAfter;

    private String referenceType;
    private Long referenceId;

    private String note;

    private Long createdByUserId;
    private String createdByUsername;
    private String createdByFullName;

    private LocalDateTime createdAt;
}