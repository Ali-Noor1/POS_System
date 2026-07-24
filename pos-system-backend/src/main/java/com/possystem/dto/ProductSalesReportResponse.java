package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ProductSalesReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalQuantitySold;
    private BigDecimal totalRevenue;
    private List<ProductSalesReportItemResponse> products;
}
