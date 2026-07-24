package com.possystem.service;

import com.possystem.dto.DashboardRecentSaleResponse;
import com.possystem.dto.DashboardStatsResponse;
import com.possystem.entity.Customer;
import com.possystem.entity.Role;
import com.possystem.entity.Sale;
import com.possystem.entity.SaleStatus;
import com.possystem.entity.User;
import com.possystem.repository.CustomerRepository;
import com.possystem.repository.ProductRepository;
import com.possystem.repository.SaleRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                saleRepository,
                productRepository,
                customerRepository
        );
    }

    @Test
    void getDashboardStats_returnsTodayCountsTotalsAndRecentSales() {

        Sale sale = recentSale();

        when(saleRepository.sumCompletedSalesTotalBetween(
                todayStart(),
                tomorrowStart()
        )).thenReturn(new BigDecimal("1250.00"));
        when(saleRepository.countByStatusAndCreatedAtBetween(
                SaleStatus.COMPLETED,
                todayStart(),
                tomorrowStart()
        )).thenReturn(5L);
        when(productRepository.countActiveLowStockProducts())
                .thenReturn(3L);
        when(productRepository.count())
                .thenReturn(25L);
        when(customerRepository.count())
                .thenReturn(12L);
        when(saleRepository.findTop5ByOrderByCreatedAtDesc())
                .thenReturn(List.of(sale));

        DashboardStatsResponse response =
                dashboardService.getDashboardStats();

        assertEquals(new BigDecimal("1250.00"), response.getTodaySalesTotal());
        assertEquals(5L, response.getTodaySaleCount());
        assertEquals(3L, response.getLowStockCount());
        assertEquals(25L, response.getTotalProducts());
        assertEquals(12L, response.getTotalCustomers());

        assertEquals(1, response.getRecentSales().size());

        DashboardRecentSaleResponse recentSale =
                response.getRecentSales().get(0);

        assertEquals(20L, recentSale.getSaleId());
        assertEquals("R-00020", recentSale.getReceiptNumber());
        assertEquals("Ali Customer", recentSale.getCustomerName());
        assertEquals("cashier_one", recentSale.getCashierUsername());
        assertEquals(SaleStatus.COMPLETED, recentSale.getSaleStatus());
        assertEquals(new BigDecimal("250.00"), recentSale.getTotalAmount());
        assertEquals(sale.getCreatedAt(), recentSale.getCreatedAt());

        ArgumentCaptor<LocalDateTime> startCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(saleRepository).sumCompletedSalesTotalBetween(
                startCaptor.capture(),
                endCaptor.capture()
        );

        assertEquals(todayStart(), startCaptor.getValue());
        assertEquals(tomorrowStart(), endCaptor.getValue());
    }

    private Sale recentSale() {
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
        ReflectionTestUtils.setField(sale, "id", 20L);
        ReflectionTestUtils.setField(
                sale,
                "createdAt",
                LocalDateTime.of(2026, 7, 5, 9, 30)
        );
        sale.setReceiptNumber("R-00020");
        sale.setCustomer(customer);
        sale.setCashier(cashier);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setTotalAmount(new BigDecimal("250.00"));

        return sale;
    }

    private LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }

    private LocalDateTime tomorrowStart() {
        return todayStart().plusDays(1);
    }
}
