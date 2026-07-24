package com.possystem.controller;

import com.possystem.dto.SupplierRequest;
import com.possystem.dto.SupplierResponse;
import com.possystem.dto.SupplierStatusRequest;
import com.possystem.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse createSupplier(
            @Valid @RequestBody SupplierRequest request
    ) {
        return supplierService.createSupplier(request);
    }

    @GetMapping
    public List<SupplierResponse> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/active")
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierService.getActiveSuppliers();
    }

    @GetMapping("/{id}")
    public SupplierResponse getSupplierById(
            @PathVariable Long id
    ) {
        return supplierService.getSupplierById(id);
    }

    @GetMapping("/search")
    public List<SupplierResponse> searchSuppliers(
            @RequestParam String query
    ) {
        return supplierService.searchSuppliers(query);
    }

    @PutMapping("/{id}")
    public SupplierResponse updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request
    ) {
        return supplierService.updateSupplier(id, request);
    }

    @PatchMapping("/{id}/status")
    public SupplierResponse updateSupplierStatus(
            @PathVariable Long id,
            @Valid @RequestBody SupplierStatusRequest request
    ) {
        return supplierService.updateSupplierStatus(
                id,
                request.getStatus()
        );
    }
}