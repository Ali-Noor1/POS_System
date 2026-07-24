package com.possystem.repository;

import com.possystem.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleIdOrderByIdAsc(Long saleId);

    @Query("""
        SELECT new com.possystem.repository.ProductSalesReportRow(
            si.product.id,
            si.productName,
            si.productSku,
            SUM(si.quantity),
            SUM(si.lineTotal)
        )
        FROM SaleItem si
        WHERE si.sale.status = 'COMPLETED'
          AND si.sale.createdAt >= :start
          AND si.sale.createdAt < :end
        GROUP BY si.product.id, si.productName, si.productSku
        ORDER BY SUM(si.lineTotal) DESC, si.productName ASC
        """)
    List<ProductSalesReportRow> findProductSalesReportRows(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
