package com.possystem.service;

import com.possystem.dto.SupplierRequest;
import com.possystem.dto.SupplierResponse;
import com.possystem.entity.Supplier;
import com.possystem.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        String name = request.getName().trim();

        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Supplier with this name already exists"
            );
        }

        Supplier supplier = Supplier.builder()
                .name(name)
                .companyName(cleanOptionalText(request.getCompanyName()))
                .phone(cleanOptionalText(request.getPhone()))
                .email(cleanOptionalText(request.getEmail()))
                .address(cleanOptionalText(request.getAddress()))
                .status("ACTIVE")
                .build();

        Supplier savedSupplier = supplierRepository.save(supplier);

        auditLogService.record(
                "SUPPLIER_CREATED",
                "SUPPLIER",
                savedSupplier.getId(),
                "Supplier created: " + savedSupplier.getName()
        );

        return mapToSupplierResponse(savedSupplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToSupplierResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByStatusIgnoreCaseOrderByNameAsc("ACTIVE")
                .stream()
                .map(this::mapToSupplierResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        return mapToSupplierResponse(findSupplierById(id));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> searchSuppliers(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search query must contain at least 2 characters"
            );
        }

        return supplierRepository.searchSuppliers(query.trim())
                .stream()
                .map(this::mapToSupplierResponse)
                .toList();
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = findSupplierById(id);

        supplier.setName(request.getName().trim());
        supplier.setCompanyName(cleanOptionalText(request.getCompanyName()));
        supplier.setPhone(cleanOptionalText(request.getPhone()));
        supplier.setEmail(cleanOptionalText(request.getEmail()));
        supplier.setAddress(cleanOptionalText(request.getAddress()));

        Supplier updatedSupplier = supplierRepository.saveAndFlush(supplier);

        auditLogService.record(
                "SUPPLIER_UPDATED",
                "SUPPLIER",
                updatedSupplier.getId(),
                "Supplier updated: " + updatedSupplier.getName()
        );

        return mapToSupplierResponse(updatedSupplier);
    }

    @Transactional
    public SupplierResponse updateSupplierStatus(Long id, String status) {
        Supplier supplier = findSupplierById(id);

        String newStatus = status.trim().toUpperCase(Locale.ROOT);

        if (!newStatus.equals("ACTIVE") && !newStatus.equals("INACTIVE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Supplier status must be ACTIVE or INACTIVE"
            );
        }

        supplier.setStatus(newStatus);

        Supplier updatedSupplier = supplierRepository.saveAndFlush(supplier);

        auditLogService.record(
                "SUPPLIER_STATUS_UPDATED",
                "SUPPLIER",
                updatedSupplier.getId(),
                "Supplier status changed to " + newStatus
        );

        return mapToSupplierResponse(updatedSupplier);
    }

    public Supplier findSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found with ID: " + id
                ));
    }

    private SupplierResponse mapToSupplierResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .companyName(supplier.getCompanyName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    private String cleanOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}