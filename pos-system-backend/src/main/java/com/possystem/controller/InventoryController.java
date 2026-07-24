package com.possystem.controller;

import com.possystem.dto.InventoryAdjustmentRequest;
import com.possystem.dto.InventoryAdjustmentResponse;
import com.possystem.dto.LowStockProductResponse;
import com.possystem.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.possystem.dto.InventoryTransactionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryAdjustmentResponse adjustStock(
            @Valid @RequestBody InventoryAdjustmentRequest request,
            Authentication authentication
    ) {
        return inventoryService.adjustStock(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/products/{productId}/transactions")
    public List<InventoryTransactionResponse> getProductTransactions(
            @PathVariable Long productId
    ) {
        return inventoryService.getProductTransactions(productId);
    }

    @GetMapping("/low-stock")
    public List<LowStockProductResponse> getLowStockProducts() {
        return inventoryService.getLowStockProducts();
    }
}