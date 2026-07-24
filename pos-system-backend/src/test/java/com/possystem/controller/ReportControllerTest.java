package com.possystem.controller;

import com.possystem.dto.InventoryMovementReportResponse;
import com.possystem.dto.ProductSalesReportResponse;
import com.possystem.dto.SalesReportResponse;
import com.possystem.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @Test
    void getSalesReport_returnsSalesReportResponse() {

        ReportController controller = new ReportController(reportService);

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        SalesReportResponse serviceResponse = SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .grossSalesTotal(new BigDecimal("500.00"))
                .completedSaleCount(2L)
                .cancelledSaleCount(1L)
                .sales(List.of())
                .build();

        when(reportService.getSalesReport(startDate, endDate))
                .thenReturn(serviceResponse);

        SalesReportResponse response = controller.getSalesReport(
                startDate,
                endDate
        );

        assertSame(serviceResponse, response);
        verify(reportService).getSalesReport(startDate, endDate);
    }

    @Test
    void getProductSalesReport_returnsProductSalesReportResponse() {

        ReportController controller = new ReportController(reportService);

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        ProductSalesReportResponse serviceResponse =
                ProductSalesReportResponse.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .totalQuantitySold(new BigDecimal("5.000"))
                        .totalRevenue(new BigDecimal("500.00"))
                        .products(List.of())
                        .build();

        when(reportService.getProductSalesReport(startDate, endDate))
                .thenReturn(serviceResponse);

        ProductSalesReportResponse response =
                controller.getProductSalesReport(startDate, endDate);

        assertSame(serviceResponse, response);
        verify(reportService).getProductSalesReport(startDate, endDate);
    }

    @Test
    void getInventoryMovementReport_returnsInventoryMovementReportResponse() {

        ReportController controller = new ReportController(reportService);

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        InventoryMovementReportResponse serviceResponse =
                InventoryMovementReportResponse.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .totalInQuantity(new BigDecimal("10.000"))
                        .totalOutQuantity(new BigDecimal("2.000"))
                        .netQuantityChange(new BigDecimal("8.000"))
                        .movements(List.of())
                        .build();

        when(reportService.getInventoryMovementReport(startDate, endDate))
                .thenReturn(serviceResponse);

        InventoryMovementReportResponse response =
                controller.getInventoryMovementReport(startDate, endDate);

        assertSame(serviceResponse, response);
        verify(reportService).getInventoryMovementReport(startDate, endDate);
    }
}
