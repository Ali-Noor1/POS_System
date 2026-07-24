package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardStatsResponse {

    private BigDecimal todaySalesTotal;
    private Long todaySaleCount;
    private Long lowStockCount;
    private Long totalProducts;
    private Long totalCustomers;
    private List<DashboardRecentSaleResponse> recentSales;
}
