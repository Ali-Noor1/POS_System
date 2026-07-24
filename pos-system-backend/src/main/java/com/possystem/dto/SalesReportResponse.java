package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SalesReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal grossSalesTotal;
    private Long completedSaleCount;
    private Long cancelledSaleCount;
    private List<SalesReportSaleResponse> sales;
}
