package com.possystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CompleteSaleRequest {

    /*
     * Optional:
     * null means walk-in customer.
     * A positive ID means a registered customer was selected.
     */
    @Positive(message = "Customer ID must be greater than zero")
    private Long customerId;

    @NotEmpty(message = "At least one sale item is required")
    @Valid
    private List<CompleteSaleItemRequest> items;

    @NotNull(message = "Payment information is required")
    @Valid
    private CompleteSalePaymentRequest payment;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<CompleteSaleItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CompleteSaleItemRequest> items) {
        this.items = items;
    }

    public CompleteSalePaymentRequest getPayment() {
        return payment;
    }

    public void setPayment(CompleteSalePaymentRequest payment) {
        this.payment = payment;
    }
}