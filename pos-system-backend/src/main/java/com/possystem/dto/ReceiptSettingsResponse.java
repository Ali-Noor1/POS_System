package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReceiptSettingsResponse {

    private String headerText;
    private String footerText;
    private BigDecimal taxPercentage;
    private String currencySymbol;
    private Boolean showCashierName;
    private Boolean showCustomerInfo;
}
