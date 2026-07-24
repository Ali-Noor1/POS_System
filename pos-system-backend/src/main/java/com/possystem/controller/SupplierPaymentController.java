package com.possystem.controller;

import com.possystem.dto.SupplierPaymentRequest;
import com.possystem.dto.SupplierPaymentResponse;
import com.possystem.service.SupplierPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier-payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SupplierPaymentController {

    private final SupplierPaymentService supplierPaymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierPaymentResponse createSupplierPayment(
            @Valid @RequestBody SupplierPaymentRequest request
    ) {
        return supplierPaymentService.createSupplierPayment(request);
    }

    @GetMapping
    public List<SupplierPaymentResponse> getAllSupplierPayments() {
        return supplierPaymentService.getAllSupplierPayments();
    }

    @GetMapping("/supplier/{supplierId}")
    public List<SupplierPaymentResponse> getSupplierPayments(
            @PathVariable Long supplierId
    ) {
        return supplierPaymentService.getSupplierPayments(supplierId);
    }

    @GetMapping("/purchase/{purchaseId}")
    public List<SupplierPaymentResponse> getPurchasePayments(
            @PathVariable Long purchaseId
    ) {
        return supplierPaymentService.getPurchasePayments(purchaseId);
    }
}