package com.possystem.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sale_items",
        indexes = {
                @Index(name = "idx_sale_items_sale_id", columnList = "sale_id"),
                @Index(name = "idx_sale_items_product_id", columnList = "product_id")
        }
)
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One sale can contain many sale items.
     * Every sale item must belong to one sale.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    /*
     * Product that was sold.
     * We keep this relationship for reporting and stock/audit traceability.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * Snapshot fields.
     * These protect old receipts if the product name or SKU changes later.
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;

    /*
     * Quantity supports values such as:
     * 1.000
     * 2.000
     * 0.500
     *
     * This keeps the POS suitable for grocery/weight-based products later.
     */
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    /*
     * Selling price of one unit at the exact time of sale.
     * It must not change if product sellingPrice changes later.
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /*
     * Reserved for future discount functionality.
     * For now it will remain 0.00.
     */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /*
     * Final total for this one line/item.
     *
     * Later:
     * lineTotal = (unitPrice × quantity) - discountAmount
     */
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.quantity == null) {
            this.quantity = BigDecimal.ZERO;
        }

        if (this.unitPrice == null) {
            this.unitPrice = BigDecimal.ZERO;
        }

        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }

        if (this.lineTotal == null) {
            this.lineTotal = BigDecimal.ZERO;
        }
    }

    public Long getId() {
        return id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}