package com.possystem.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_transactions",
        indexes = {
                @Index(
                        name = "idx_inventory_transactions_product_id",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_inventory_transactions_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_inventory_transactions_created_by",
                        columnList = "created_by"
                )
        }
)
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * The product whose stock changed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * Type of stock movement:
     * PURCHASE, SALE, ADJUSTMENT_IN, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    /*
     * Positive number = stock added.
     * Negative number = stock removed.
     *
     * Examples:
     * +10.000 for a purchase
     * -2.000 for a sale
     */
    @Column(name = "quantity_change", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityChange;

    /*
     * Product stock immediately before this transaction.
     */
    @Column(name = "stock_before", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockBefore;

    /*
     * Product stock immediately after this transaction.
     */
    @Column(name = "stock_after", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockAfter;

    /*
     * Will be useful later for connections such as:
     * SALE → sale ID
     * PURCHASE → purchase ID
     * ADJUSTMENT → no reference needed initially
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 1000)
    private String note;

    /*
     * The Admin or Cashier who caused this stock change.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InventoryTransaction() {
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public InventoryTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(
            InventoryTransactionType transactionType
    ) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(BigDecimal stockBefore) {
        this.stockBefore = stockBefore;
    }

    public BigDecimal getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(BigDecimal stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}