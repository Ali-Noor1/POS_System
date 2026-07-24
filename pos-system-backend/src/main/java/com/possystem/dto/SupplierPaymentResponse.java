package com.possystem.dto;

import com.possystem.entity.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SupplierPaymentResponse {

    private Long id;

    private Long supplierId;
    private String supplierName;

    private Long purchaseId;
    private String purchaseNumber;

    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String referenceNumber;
    private String note;

    private Long paidById;
    private String paidByUsername;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}