package com.possystem.repository;

import com.possystem.entity.SupplierPayment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    @EntityGraph(attributePaths = {"supplier", "purchase", "paidBy"})
    List<SupplierPayment> findAllByOrderByPaidAtDesc();

    @EntityGraph(attributePaths = {"supplier", "purchase", "paidBy"})
    List<SupplierPayment> findBySupplier_IdOrderByPaidAtDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "purchase", "paidBy"})
    List<SupplierPayment> findByPurchase_IdOrderByPaidAtDesc(Long purchaseId);

    @EntityGraph(attributePaths = {"supplier", "purchase", "paidBy"})
    List<SupplierPayment> findByPaidAtBetweenOrderByPaidAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );
}