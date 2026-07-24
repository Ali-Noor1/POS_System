package com.possystem.dto;

import com.possystem.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPaymentRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private Long purchaseId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}