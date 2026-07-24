package com.possystem.controller;

import com.possystem.dto.PurchaseRequest;
import com.possystem.dto.PurchaseResponse;
import com.possystem.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse createPurchase(
            @Valid @RequestBody PurchaseRequest request
    ) {
        return purchaseService.createPurchase(request);
    }

    @GetMapping
    public List<PurchaseResponse> getAllPurchases() {
        return purchaseService.getAllPurchases();
    }

    @GetMapping("/{id}")
    public PurchaseResponse getPurchaseById(
            @PathVariable Long id
    ) {
        return purchaseService.getPurchaseById(id);
    }

    @GetMapping("/supplier/{supplierId}")
    public List<PurchaseResponse> getPurchasesBySupplier(
            @PathVariable Long supplierId
    ) {
        return purchaseService.getPurchasesBySupplier(supplierId);
    }

    @GetMapping("/date-range")
    public List<PurchaseResponse> getPurchasesByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return purchaseService.getPurchasesBetween(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );
    }
}