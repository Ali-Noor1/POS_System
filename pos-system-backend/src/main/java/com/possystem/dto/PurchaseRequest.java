package com.possystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @DecimalMin(value = "0.00", inclusive = true, message = "Discount cannot be negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.00", inclusive = true, message = "Paid amount cannot be negative")
    private BigDecimal paidAmount;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    @Valid
    @NotEmpty(message = "Purchase must contain at least one item")
    private List<PurchaseItemRequest> items;
}