package com.possystem.dto;

import com.possystem.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CompleteSalePaymentRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /*
     * For CASH:
     * amountReceived may be greater than the sale total.
     * The backend will later calculate change.
     *
     * For CARD, BANK_TRANSFER, and MOBILE_WALLET:
     * amountReceived must later equal the final sale total.
     */
    @NotNull(message = "Amount received is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount received must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount received can have a maximum of 2 decimal places")
    private BigDecimal amountReceived;

    /*
     * Optional for cash.
     * Later useful for card transaction ID, bank transfer ID,
     * wallet transaction ID, etc.
     */
    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNumber;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
}