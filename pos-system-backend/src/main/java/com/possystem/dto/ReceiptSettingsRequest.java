package com.possystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReceiptSettingsRequest {

    @Size(max = 250, message = "Receipt header must not exceed 250 characters")
    private String headerText;

    @Size(max = 250, message = "Receipt footer must not exceed 250 characters")
    private String footerText;

    @NotNull(message = "Tax percentage is required")
    @DecimalMin(value = "0.00", message = "Tax percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Tax percentage cannot exceed 100")
    private BigDecimal taxPercentage;

    @NotBlank(message = "Currency symbol is required")
    @Size(max = 10, message = "Currency symbol must not exceed 10 characters")
    private String currencySymbol;

    @NotNull(message = "Show cashier name setting is required")
    private Boolean showCashierName;

    @NotNull(message = "Show customer info setting is required")
    private Boolean showCustomerInfo;
}
