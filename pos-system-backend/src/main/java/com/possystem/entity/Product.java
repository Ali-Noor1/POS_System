package com.possystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uk_products_barcode", columnNames = "barcode")
        },
        indexes = {
                @Index(name = "idx_products_category_id", columnList = "category_id"),
                @Index(name = "idx_products_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 100)
    private String barcode;

    @Column(length = 100)
    private String brand;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentStock;

    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 3)
    private BigDecimal reorderLevel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.costPrice == null) {
            this.costPrice = BigDecimal.ZERO;
        }

        if (this.sellingPrice == null) {
            this.sellingPrice = BigDecimal.ZERO;
        }

        if (this.currentStock == null) {
            this.currentStock = BigDecimal.ZERO;
        }

        if (this.reorderLevel == null) {
            this.reorderLevel = BigDecimal.ZERO;
        }

        if (this.status == null || this.status.isBlank()) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}