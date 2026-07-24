package com.possystem.service;

import com.possystem.dto.ProductSalesReportItemResponse;
import com.possystem.dto.ProductSalesReportResponse;
import com.possystem.dto.InventoryMovementReportResponse;
import com.possystem.dto.InventoryTransactionResponse;
import com.possystem.dto.SalesReportResponse;
import com.possystem.dto.SalesReportSaleResponse;
import com.possystem.entity.InventoryTransaction;
import com.possystem.entity.InventoryTransactionType;
import com.possystem.repository.ProductSalesReportRow;
import com.possystem.entity.Product;
import com.possystem.entity.Customer;
import com.possystem.entity.Role;
import com.possystem.entity.Sale;
import com.possystem.entity.SaleStatus;
import com.possystem.entity.User;
import com.possystem.repository.InventoryTransactionRepository;
import com.possystem.repository.SaleRepository;
import com.possystem.repository.SaleItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                saleRepository,
                saleItemRepository,
                inventoryTransactionRepository
        );
    }

    @Test
    void getSalesReport_returnsSalesWithinDateRangeWithTotals() {

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        Sale completedSale = sale(
                20L,
                "R-00020",
                SaleStatus.COMPLETED,
                new BigDecimal("300.00"),
                LocalDateTime.of(2026, 7, 3, 10, 15)
        );

        Sale cancelledSale = sale(
                21L,
                "R-00021",
                SaleStatus.CANCELLED,
                new BigDecimal("200.00"),
                LocalDateTime.of(2026, 7, 4, 11, 30)
        );

        when(saleRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(cancelledSale, completedSale));

        SalesReportResponse response = reportService.getSalesReport(
                startDate,
                endDate
        );

        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(new BigDecimal("300.00"), response.getGrossSalesTotal());
        assertEquals(1L, response.getCompletedSaleCount());
        assertEquals(1L, response.getCancelledSaleCount());
        assertEquals(2, response.getSales().size());

        SalesReportSaleResponse firstSale = response.getSales().get(0);
        assertEquals(21L, firstSale.getSaleId());
        assertEquals("R-00021", firstSale.getReceiptNumber());
        assertEquals("Ali Customer", firstSale.getCustomerName());
        assertEquals("cashier_one", firstSale.getCashierUsername());
        assertEquals(SaleStatus.CANCELLED, firstSale.getSaleStatus());
        assertEquals(new BigDecimal("200.00"), firstSale.getTotalAmount());
        assertEquals(cancelledSale.getCreatedAt(), firstSale.getCreatedAt());

        ArgumentCaptor<LocalDateTime> startCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(saleRepository).findByCreatedAtBetweenOrderByCreatedAtDesc(
                startCaptor.capture(),
                endCaptor.capture()
        );

        assertEquals(startDate.atStartOfDay(), startCaptor.getValue());
        assertEquals(endDate.plusDays(1).atStartOfDay(), endCaptor.getValue());
    }

    @Test
    void getProductSalesReport_returnsAggregatedProductSales() {

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        ProductSalesReportRow row = new ProductSalesReportRow(
                7L,
                "Perfume Bottle",
                "PERF-001",
                new BigDecimal("5.000"),
                new BigDecimal("500.00")
        );

        when(saleItemRepository.findProductSalesReportRows(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(row));

        ProductSalesReportResponse response =
                reportService.getProductSalesReport(startDate, endDate);

        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(new BigDecimal("5.000"), response.getTotalQuantitySold());
        assertEquals(new BigDecimal("500.00"), response.getTotalRevenue());
        assertEquals(1, response.getProducts().size());

        ProductSalesReportItemResponse item = response.getProducts().get(0);
        assertEquals(7L, item.getProductId());
        assertEquals("Perfume Bottle", item.getProductName());
        assertEquals("PERF-001", item.getProductSku());
        assertEquals(new BigDecimal("5.000"), item.getQuantitySold());
        assertEquals(new BigDecimal("500.00"), item.getRevenue());

        verify(saleItemRepository).findProductSalesReportRows(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
    }

    @Test
    void getInventoryMovementReport_returnsMovementsWithinDateRangeWithTotals() {

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        InventoryTransaction adjustmentIn = inventoryTransaction(
                90L,
                7L,
                "Perfume Bottle",
                InventoryTransactionType.ADJUSTMENT_IN,
                new BigDecimal("10.000"),
                new BigDecimal("5.000"),
                new BigDecimal("15.000"),
                "MANUAL_ADJUSTMENT",
                null,
                "Opening stock",
                LocalDateTime.of(2026, 7, 2, 9, 30)
        );

        InventoryTransaction saleMovement = inventoryTransaction(
                91L,
                7L,
                "Perfume Bottle",
                InventoryTransactionType.SALE,
                new BigDecimal("-2.000"),
                new BigDecimal("15.000"),
                new BigDecimal("13.000"),
                "SALE",
                20L,
                "Stock reduced for sale R-00020",
                LocalDateTime.of(2026, 7, 3, 10, 15)
        );

        when(inventoryTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(saleMovement, adjustmentIn));

        InventoryMovementReportResponse response =
                reportService.getInventoryMovementReport(startDate, endDate);

        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(new BigDecimal("10.000"), response.getTotalInQuantity());
        assertEquals(new BigDecimal("2.000"), response.getTotalOutQuantity());
        assertEquals(new BigDecimal("8.000"), response.getNetQuantityChange());
        assertEquals(2, response.getMovements().size());

        InventoryTransactionResponse firstMovement =
                response.getMovements().get(0);
        assertEquals(91L, firstMovement.getId());
        assertEquals(7L, firstMovement.getProductId());
        assertEquals("Perfume Bottle", firstMovement.getProductName());
        assertEquals(
                InventoryTransactionType.SALE,
                firstMovement.getTransactionType()
        );
        assertEquals(
                new BigDecimal("-2.000"),
                firstMovement.getQuantityChange()
        );
        assertEquals(new BigDecimal("15.000"), firstMovement.getStockBefore());
        assertEquals(new BigDecimal("13.000"), firstMovement.getStockAfter());
        assertEquals("SALE", firstMovement.getReferenceType());
        assertEquals(20L, firstMovement.getReferenceId());
        assertEquals(
                "Stock reduced for sale R-00020",
                firstMovement.getNote()
        );
        assertEquals(10L, firstMovement.getCreatedByUserId());
        assertEquals("cashier_one", firstMovement.getCreatedByUsername());
        assertEquals("Cashier One", firstMovement.getCreatedByFullName());
        assertEquals(
                saleMovement.getCreatedAt(),
                firstMovement.getCreatedAt()
        );

        verify(inventoryTransactionRepository)
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                );
    }

    @Test
    void getSalesReport_rejectsStartDateAfterEndDate() {

        LocalDate startDate = LocalDate.of(2026, 7, 5);
        LocalDate endDate = LocalDate.of(2026, 7, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getSalesReport(startDate, endDate)
        );

        assertEquals(
                "Start date must be before or equal to end date",
                exception.getMessage()
        );
        verify(saleRepository, never())
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                );
    }

    @Test
    void getProductSalesReport_rejectsStartDateAfterEndDate() {

        LocalDate startDate = LocalDate.of(2026, 7, 5);
        LocalDate endDate = LocalDate.of(2026, 7, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getProductSalesReport(startDate, endDate)
        );

        assertEquals(
                "Start date must be before or equal to end date",
                exception.getMessage()
        );
        verify(saleItemRepository, never()).findProductSalesReportRows(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
    }

    @Test
    void getInventoryMovementReport_rejectsStartDateAfterEndDate() {

        LocalDate startDate = LocalDate.of(2026, 7, 5);
        LocalDate endDate = LocalDate.of(2026, 7, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getInventoryMovementReport(
                        startDate,
                        endDate
                )
        );

        assertEquals(
                "Start date must be before or equal to end date",
                exception.getMessage()
        );
        verify(inventoryTransactionRepository, never())
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                );
    }

    private Sale sale(
            Long id,
            String receiptNumber,
            SaleStatus status,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
        Role cashierRole = new Role("CASHIER", "Cashier role");
        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        Customer customer = Customer.builder()
                .fullName("Ali Customer")
                .phone("+923001234567")
                .build();
        ReflectionTestUtils.setField(customer, "id", 15L);

        Sale sale = new Sale();
        ReflectionTestUtils.setField(sale, "id", id);
        ReflectionTestUtils.setField(sale, "createdAt", createdAt);
        sale.setReceiptNumber(receiptNumber);
        sale.setCustomer(customer);
        sale.setCashier(cashier);
        sale.setStatus(status);
        sale.setTotalAmount(totalAmount);

        return sale;
    }

    private InventoryTransaction inventoryTransaction(
            Long id,
            Long productId,
            String productName,
            InventoryTransactionType transactionType,
            BigDecimal quantityChange,
            BigDecimal stockBefore,
            BigDecimal stockAfter,
            String referenceType,
            Long referenceId,
            String note,
            LocalDateTime createdAt
    ) {
        Role cashierRole = new Role("CASHIER", "Cashier role");
        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        Product product = Product.builder()
                .name(productName)
                .build();
        ReflectionTestUtils.setField(product, "id", productId);

        InventoryTransaction transaction = new InventoryTransaction();
        ReflectionTestUtils.setField(transaction, "id", id);
        ReflectionTestUtils.setField(transaction, "createdAt", createdAt);
        transaction.setProduct(product);
        transaction.setTransactionType(transactionType);
        transaction.setQuantityChange(quantityChange);
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(stockAfter);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setNote(note);
        transaction.setCreatedBy(cashier);

        return transaction;
    }
}
