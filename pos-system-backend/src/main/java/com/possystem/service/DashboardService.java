package com.possystem.service;

import com.possystem.dto.DashboardRecentSaleResponse;
import com.possystem.dto.DashboardStatsResponse;
import com.possystem.entity.Sale;
import com.possystem.entity.SaleStatus;
import com.possystem.repository.CustomerRepository;
import com.possystem.repository.ProductRepository;
import com.possystem.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public DashboardService(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository
    ) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        BigDecimal todaySalesTotal =
                saleRepository.sumCompletedSalesTotalBetween(
                        todayStart,
                        tomorrowStart
                );

        List<DashboardRecentSaleResponse> recentSales =
                saleRepository.findTop5ByOrderByCreatedAtDesc()
                        .stream()
                        .map(this::mapToRecentSaleResponse)
                        .toList();

        return DashboardStatsResponse.builder()
                .todaySalesTotal(todaySalesTotal)
                .todaySaleCount(
                        saleRepository.countByStatusAndCreatedAtBetween(
                                SaleStatus.COMPLETED,
                                todayStart,
                                tomorrowStart
                        )
                )
                .lowStockCount(productRepository.countActiveLowStockProducts())
                .totalProducts(productRepository.count())
                .totalCustomers(customerRepository.count())
                .recentSales(recentSales)
                .build();
    }

    private DashboardRecentSaleResponse mapToRecentSaleResponse(Sale sale) {
        String customerName = null;

        if (sale.getCustomer() != null) {
            customerName = sale.getCustomer().getFullName();
        }

        return DashboardRecentSaleResponse.builder()
                .saleId(sale.getId())
                .receiptNumber(sale.getReceiptNumber())
                .customerName(customerName)
                .cashierUsername(sale.getCashier().getUsername())
                .saleStatus(sale.getStatus())
                .totalAmount(sale.getTotalAmount())
                .createdAt(sale.getCreatedAt())
                .build();
    }
}
