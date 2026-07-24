package com.possystem.repository;

import com.possystem.entity.PurchaseItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<PurchaseItem> findByPurchase_IdOrderByIdAsc(Long purchaseId);

    @EntityGraph(attributePaths = {"purchase", "purchase.supplier"})
    List<PurchaseItem> findByProduct_IdOrderByCreatedAtDesc(Long productId);
}