package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LowStockProductResponse {

    private Long productId;

    private String name;
    private String sku;
    private String barcode;
    private String brand;
    private String imageUrl;

    private Long categoryId;
    private String categoryName;

    private BigDecimal currentStock;
    private BigDecimal reorderLevel;

    /*
     * How many units are needed to reach the reorder level.
     *
     * Example:
     * currentStock = 2
     * reorderLevel = 5
     * shortageQuantity = 3
     */
    private BigDecimal shortageQuantity;
}