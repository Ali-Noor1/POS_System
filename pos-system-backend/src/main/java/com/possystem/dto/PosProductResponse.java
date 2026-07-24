package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PosProductResponse {

    private Long id;

    private Long categoryId;
    private String categoryName;

    private String name;
    private String sku;
    private String barcode;
    private String brand;
    private String description;
    private String imageUrl;

    private BigDecimal sellingPrice;
    private BigDecimal currentStock;
}