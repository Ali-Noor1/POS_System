package com.possystem.repository;

import com.possystem.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Supplier> findAllByOrderByCreatedAtDesc();

    List<Supplier> findByStatusIgnoreCaseOrderByNameAsc(String status);

    @Query("""
        SELECT s
        FROM Supplier s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY s.name ASC
        """)
    List<Supplier> searchSuppliers(
            @Param("query") String query
    );
}