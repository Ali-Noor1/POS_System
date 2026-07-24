package com.possystem.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_sale_id", columnList = "sale_id"),
                @Index(name = "idx_payments_status", columnList = "payment_status"),
                @Index(name = "idx_payments_method", columnList = "payment_method"),
                @Index(name = "idx_payments_created_at", columnList = "created_at")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Each payment belongs to one completed sale.
     *
     * We do not make sale_id unique, because later we may support:
     * - split payment
     * - partial cash + card payment
     * - multiple payment records for one sale
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    /*
     * Amount received through this payment method.
     *
     * Example:
     * CASH = 5,000.00
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;


    /*
     * Total money received from the customer.
     *
     * For CASH:
     * amountReceived can be greater than the sale total.
     *
     * For CARD, BANK_TRANSFER, and MOBILE_WALLET:
     * amountReceived should later equal the payment amount.
     */
    @Column(name = "amount_received", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountReceived = BigDecimal.ZERO;

    /*
     * Change returned to the customer.
     *
     * For CASH:
     * changeAmount = amountReceived - amount
     *
     * For non-cash payments:
     * changeAmount = 0.00
     */
    @Column(name = "change_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    /*
     * Optional reference for card transaction, bank transfer,
     * wallet transaction, etc.
     *
     * Cash payments normally keep this null.
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    /*
     * Actual date/time when payment is accepted.
     */
    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.paidAt = now;
        this.createdAt = now;
        this.updatedAt = now;

        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PAID;
        }

        if (this.amount == null) {
            this.amount = BigDecimal.ZERO;
        }

        if (this.amountReceived == null) {
            this.amountReceived = BigDecimal.ZERO;
        }

        if (this.changeAmount == null) {
            this.changeAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}