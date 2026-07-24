package com.possystem.controller;

import com.possystem.dto.CancelSaleRequest;
import com.possystem.dto.CompleteSaleRequest;
import com.possystem.dto.CompleteSaleResponse;
import com.possystem.dto.SaleHistoryResponse;
import com.possystem.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    // =========================================================
    // COMPLETE SALE
    // =========================================================

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<CompleteSaleResponse> completeSale(
            @Valid @RequestBody CompleteSaleRequest request
    ) {
        CompleteSaleResponse response =
                saleService.completeSale(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // SALES HISTORY
    // =========================================================

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<List<SaleHistoryResponse>> getSalesHistory() {

        List<SaleHistoryResponse> response =
                saleService.getSalesHistory();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // CANCEL SALE
    // =========================================================

    @PatchMapping("/{saleId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompleteSaleResponse> cancelSale(
            @PathVariable Long saleId,
            @Valid @RequestBody CancelSaleRequest request
    ) {
        CompleteSaleResponse response = saleService.cancelSale(
                saleId,
                request
        );

        return ResponseEntity.ok(response);
    }
    // =========================================================
    // SALE RECEIPT DETAIL
    // =========================================================

    @GetMapping("/{saleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<CompleteSaleResponse> getSaleDetails(
            @PathVariable Long saleId
    ) {
        CompleteSaleResponse response =
                saleService.getSaleDetails(saleId);

        return ResponseEntity.ok(response);
    }
}