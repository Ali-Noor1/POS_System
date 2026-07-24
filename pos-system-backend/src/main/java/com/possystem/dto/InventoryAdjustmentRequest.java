package com.possystem.dto;

import com.possystem.entity.InventoryTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InventoryAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Transaction type is required")
    private InventoryTransactionType transactionType;

    /*
     * Always send a positive quantity.
     *
     * ADJUSTMENT_IN  + quantity → stock increases
     * ADJUSTMENT_OUT + quantity → stock decreases
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(
            value = "0.001",
            message = "Quantity must be greater than zero"
    )
    @Digits(
            integer = 9,
            fraction = 3,
            message = "Quantity can have up to 9 whole digits and 3 decimal places"
    )
    private BigDecimal quantity;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}