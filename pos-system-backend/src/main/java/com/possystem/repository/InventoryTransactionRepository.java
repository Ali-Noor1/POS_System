package com.possystem.repository;

import com.possystem.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProduct_IdOrderByCreatedAtDesc(
            Long productId
    );

    List<InventoryTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );
}
