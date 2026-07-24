package com.possystem.controller;

import com.possystem.dto.InventoryMovementReportResponse;
import com.possystem.dto.ProductSalesReportResponse;
import com.possystem.dto.SalesReportResponse;
import com.possystem.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public SalesReportResponse getSalesReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return reportService.getSalesReport(startDate, endDate);
    }

    @GetMapping("/product-sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductSalesReportResponse getProductSalesReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return reportService.getProductSalesReport(startDate, endDate);
    }

    @GetMapping("/inventory-movements")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryMovementReportResponse getInventoryMovementReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return reportService.getInventoryMovementReport(startDate, endDate);
    }
}
