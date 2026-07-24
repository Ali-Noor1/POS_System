package com.possystem.repository;

import com.possystem.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySkuIgnoreCase(String sku);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByBarcode(String barcode);

    @EntityGraph(attributePaths = "category")
    List<Product> findByStatusIgnoreCase(String status);

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryIdAndStatusIgnoreCase(
            Long categoryId,
            String status
    );

    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT p
        FROM Product p
        WHERE UPPER(p.status) = 'ACTIVE'
        ORDER BY p.name ASC
        """)
    List<Product> findActiveProductsOrderByName();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(
            @Param("id") Long id
    );

    @Query("""
        SELECT p
        FROM Product p
        WHERE UPPER(p.status) = 'ACTIVE'
          AND p.currentStock <= p.reorderLevel
        ORDER BY p.currentStock ASC, p.name ASC
        """)
    List<Product> findActiveLowStockProducts();

    @Query("""
        SELECT COUNT(p)
        FROM Product p
        WHERE UPPER(p.status) = 'ACTIVE'
          AND p.currentStock <= p.reorderLevel
        """)
    long countActiveLowStockProducts();


    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT p
        FROM Product p
        WHERE UPPER(p.status) = 'ACTIVE'
          AND (
              LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY p.name ASC
        """)
    List<Product> searchActiveProductsByNameOrSku(
            @Param("query") String query
    );

    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.barcode = :barcode
          AND UPPER(p.status) = 'ACTIVE'
        """)
    Optional<Product> findActiveByBarcode(
            @Param("barcode") String barcode
    );

}
