package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductSalesReportItemResponse {

    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal quantitySold;
    private BigDecimal revenue;
}
