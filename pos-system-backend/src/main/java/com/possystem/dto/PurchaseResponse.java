package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PurchaseResponse {

    private Long id;
    private String purchaseNumber;

    private Long supplierId;
    private String supplierName;

    private String status;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;

    private String note;

    private Long createdById;
    private String createdByUsername;

    private List<Item> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class Item {
        private Long id;

        private Long productId;
        private String productName;
        private String productSku;

        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal lineTotal;
    }
}