package com.possystem.repository;

import com.possystem.entity.Sale;
import com.possystem.entity.SaleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    /*
     * ADMIN sales history.
     * Customer and cashier are loaded with each sale.
     */
    @EntityGraph(attributePaths = {"customer", "cashier"})
    List<Sale> findAllByOrderByCreatedAtDesc();

    /*
     * CASHIER sales history.
     * A cashier sees only sales completed by that cashier.
     */
    @EntityGraph(attributePaths = {"customer", "cashier"})
    List<Sale> findByCashier_IdOrderByCreatedAtDesc(Long cashierId);

    @Query("""
        SELECT COALESCE(SUM(s.totalAmount), 0)
        FROM Sale s
        WHERE s.status = 'COMPLETED'
          AND s.createdAt >= :start
          AND s.createdAt < :end
        """)
    BigDecimal sumCompletedSalesTotalBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByStatusAndCreatedAtBetween(
            SaleStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = {"customer", "cashier"})
    List<Sale> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "cashier"})
    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );
}
