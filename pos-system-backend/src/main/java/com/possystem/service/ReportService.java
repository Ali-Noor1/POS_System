package com.possystem.service;

import com.possystem.dto.InventoryMovementReportResponse;
import com.possystem.dto.InventoryTransactionResponse;
import com.possystem.dto.ProductSalesReportItemResponse;
import com.possystem.dto.ProductSalesReportResponse;
import com.possystem.dto.SalesReportResponse;
import com.possystem.dto.SalesReportSaleResponse;
import com.possystem.entity.InventoryTransaction;
import com.possystem.entity.Sale;
import com.possystem.entity.SaleStatus;
import com.possystem.repository.InventoryTransactionRepository;
import com.possystem.repository.ProductSalesReportRow;
import com.possystem.repository.SaleItemRepository;
import com.possystem.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public ReportService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Sale> sales = saleRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        long completedSaleCount = sales.stream()
                .filter(sale -> sale.getStatus() == SaleStatus.COMPLETED)
                .count();

        long cancelledSaleCount = sales.stream()
                .filter(sale -> sale.getStatus() == SaleStatus.CANCELLED)
                .count();

        BigDecimal grossSalesTotal = sales.stream()
                .filter(sale -> sale.getStatus() == SaleStatus.COMPLETED)
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .grossSalesTotal(grossSalesTotal)
                .completedSaleCount(completedSaleCount)
                .cancelledSaleCount(cancelledSaleCount)
                .sales(sales.stream()
                        .map(this::mapToSalesReportSaleResponse)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductSalesReportResponse getProductSalesReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<ProductSalesReportRow> rows = saleItemRepository
                .findProductSalesReportRows(start, end);

        BigDecimal totalQuantitySold = rows.stream()
                .map(ProductSalesReportRow::getQuantitySold)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = rows.stream()
                .map(ProductSalesReportRow::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductSalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalQuantitySold(totalQuantitySold)
                .totalRevenue(totalRevenue)
                .products(rows.stream()
                        .map(this::mapToProductSalesReportItemResponse)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryMovementReportResponse getInventoryMovementReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<InventoryTransaction> movements = inventoryTransactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        BigDecimal totalInQuantity = movements.stream()
                .map(InventoryTransaction::getQuantityChange)
                .filter(quantity -> quantity.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutQuantity = movements.stream()
                .map(InventoryTransaction::getQuantityChange)
                .filter(quantity -> quantity.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netQuantityChange = movements.stream()
                .map(InventoryTransaction::getQuantityChange)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventoryMovementReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalInQuantity(totalInQuantity)
                .totalOutQuantity(totalOutQuantity)
                .netQuantityChange(netQuantityChange)
                .movements(movements.stream()
                        .map(this::mapToInventoryTransactionResponse)
                        .toList())
                .build();
    }

    private ProductSalesReportItemResponse mapToProductSalesReportItemResponse(
            ProductSalesReportRow row
    ) {
        return ProductSalesReportItemResponse.builder()
                .productId(row.getProductId())
                .productName(row.getProductName())
                .productSku(row.getProductSku())
                .quantitySold(row.getQuantitySold())
                .revenue(row.getRevenue())
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date"
            );
        }
    }

    private SalesReportSaleResponse mapToSalesReportSaleResponse(Sale sale) {
        String customerName = null;

        if (sale.getCustomer() != null) {
            customerName = sale.getCustomer().getFullName();
        }

        return SalesReportSaleResponse.builder()
                .saleId(sale.getId())
                .receiptNumber(sale.getReceiptNumber())
                .customerName(customerName)
                .cashierUsername(sale.getCashier().getUsername())
                .saleStatus(sale.getStatus())
                .totalAmount(sale.getTotalAmount())
                .createdAt(sale.getCreatedAt())
                .build();
    }

    private InventoryTransactionResponse mapToInventoryTransactionResponse(
            InventoryTransaction transaction
    ) {
        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .productId(transaction.getProduct().getId())
                .productName(transaction.getProduct().getName())
                .transactionType(transaction.getTransactionType())
                .quantityChange(transaction.getQuantityChange())
                .stockBefore(transaction.getStockBefore())
                .stockAfter(transaction.getStockAfter())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .note(transaction.getNote())
                .createdByUserId(transaction.getCreatedBy().getId())
                .createdByUsername(transaction.getCreatedBy().getUsername())
                .createdByFullName(transaction.getCreatedBy().getFullName())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
