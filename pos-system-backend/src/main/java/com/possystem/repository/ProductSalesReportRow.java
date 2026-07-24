package com.possystem.repository;

import java.math.BigDecimal;

public class ProductSalesReportRow {

    private final Long productId;
    private final String productName;
    private final String productSku;
    private final BigDecimal quantitySold;
    private final BigDecimal revenue;

    public ProductSalesReportRow(
            Long productId,
            String productName,
            String productSku,
            BigDecimal quantitySold,
            BigDecimal revenue
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public BigDecimal getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
