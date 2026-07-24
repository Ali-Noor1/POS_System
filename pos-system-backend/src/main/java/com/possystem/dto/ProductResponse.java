package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;

    private Long categoryId;
    private String categoryName;

    private String name;
    private String sku;
    private String barcode;
    private String brand;
    private String description;
    private String imageUrl;

    private BigDecimal costPrice;
    private BigDecimal sellingPrice;

    private BigDecimal currentStock;
    private BigDecimal reorderLevel;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}