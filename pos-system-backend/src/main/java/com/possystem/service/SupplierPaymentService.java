package com.possystem.service;

import com.possystem.dto.SupplierPaymentRequest;
import com.possystem.dto.SupplierPaymentResponse;
import com.possystem.entity.*;
import com.possystem.repository.PurchaseRepository;
import com.possystem.repository.SupplierPaymentRepository;
import com.possystem.repository.SupplierRepository;
import com.possystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierPaymentService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SupplierPaymentResponse createSupplierPayment(
            SupplierPaymentRequest request
    ) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found with ID: " + request.getSupplierId()
                ));

        Purchase purchase = null;

        if (request.getPurchaseId() != null) {
            purchase = purchaseRepository.findById(request.getPurchaseId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Purchase not found with ID: " + request.getPurchaseId()
                    ));

            if (!purchase.getSupplier().getId().equals(supplier.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Purchase does not belong to selected supplier"
                );
            }
        }

        User paidBy = getAuthenticatedUser();

        SupplierPayment payment = SupplierPayment.builder()
                .supplier(supplier)
                .purchase(purchase)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .referenceNumber(cleanOptionalText(request.getReferenceNumber()))
                .note(cleanOptionalText(request.getNote()))
                .paidBy(paidBy)
                .build();

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        auditLogService.record(
                "SUPPLIER_PAYMENT_CREATED",
                "SUPPLIER_PAYMENT",
                savedPayment.getId(),
                "Supplier payment created for supplier: " + supplier.getName()
        );

        return mapToSupplierPaymentResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> getAllSupplierPayments() {
        return supplierPaymentRepository.findAllByOrderByPaidAtDesc()
                .stream()
                .map(this::mapToSupplierPaymentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> getSupplierPayments(Long supplierId) {
        return supplierPaymentRepository.findBySupplier_IdOrderByPaidAtDesc(supplierId)
                .stream()
                .map(this::mapToSupplierPaymentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> getPurchasePayments(Long purchaseId) {
        return supplierPaymentRepository.findByPurchase_IdOrderByPaidAtDesc(purchaseId)
                .stream()
                .map(this::mapToSupplierPaymentResponse)
                .toList();
    }

    private SupplierPaymentResponse mapToSupplierPaymentResponse(
            SupplierPayment payment
    ) {
        return SupplierPaymentResponse.builder()
                .id(payment.getId())
                .supplierId(payment.getSupplier().getId())
                .supplierName(payment.getSupplier().getName())
                .purchaseId(payment.getPurchase() != null ? payment.getPurchase().getId() : null)
                .purchaseNumber(payment.getPurchase() != null ? payment.getPurchase().getPurchaseNumber() : null)
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .referenceNumber(payment.getReferenceNumber())
                .note(payment.getNote())
                .paidById(payment.getPaidBy().getId())
                .paidByUsername(payment.getPaidBy().getUsername())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user was not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AccessDeniedException("Inactive user cannot create supplier payment");
        }

        return user;
    }

    private String cleanOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}