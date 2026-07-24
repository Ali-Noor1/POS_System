package com.possystem.controller;

import com.possystem.dto.DashboardStatsResponse;
import com.possystem.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Test
    void getDashboardStats_returnsDashboardStatsResponse() {

        DashboardController controller = new DashboardController(
                dashboardService
        );

        DashboardStatsResponse serviceResponse =
                DashboardStatsResponse.builder()
                        .todaySalesTotal(new BigDecimal("1250.00"))
                        .todaySaleCount(5L)
                        .lowStockCount(3L)
                        .totalProducts(25L)
                        .totalCustomers(12L)
                        .recentSales(List.of())
                        .build();

        when(dashboardService.getDashboardStats())
                .thenReturn(serviceResponse);

        DashboardStatsResponse response = controller.getDashboardStats();

        assertSame(serviceResponse, response);
        verify(dashboardService).getDashboardStats();
    }
}
