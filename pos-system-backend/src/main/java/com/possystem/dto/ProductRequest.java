package com.possystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    private String barcode;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required")
    @DecimalMin(value = "0.000", inclusive = true, message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel;
}