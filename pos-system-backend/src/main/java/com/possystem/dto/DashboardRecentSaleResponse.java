package com.possystem.dto;

import com.possystem.entity.SaleStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class DashboardRecentSaleResponse {

    private Long saleId;
    private String receiptNumber;
    private String customerName;
    private String cashierUsername;
    private SaleStatus saleStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
