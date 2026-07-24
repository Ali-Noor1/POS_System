package com.possystem.repository;

import com.possystem.entity.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByPurchaseNumber(String purchaseNumber);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<Purchase> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<Purchase> findBySupplier_IdOrderByCreatedAtDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<Purchase> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<Purchase> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = {"supplier", "createdBy", "items", "items.product"})
    Optional<Purchase> findById(Long id);
}