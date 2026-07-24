package com.possystem.service;

import com.possystem.dto.PurchaseItemRequest;
import com.possystem.dto.PurchaseRequest;
import com.possystem.dto.PurchaseResponse;
import com.possystem.entity.*;
import com.possystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Purchase request is required"
            );
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found with ID: " + request.getSupplierId()
                ));

        if (!"ACTIVE".equalsIgnoreCase(supplier.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot create purchase for inactive supplier"
            );
        }

        User currentUser = getAuthenticatedUser();

        Map<Long, PreparedPurchaseItem> preparedItems = preparePurchaseItems(request.getItems());

        BigDecimal subtotal = preparedItems.values()
                .stream()
                .map(PreparedPurchaseItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = zeroIfNull(request.getDiscountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        if (discountAmount.compareTo(subtotal) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount cannot be greater than subtotal"
            );
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal paidAmount = zeroIfNull(request.getPaidAmount())
                .setScale(2, RoundingMode.HALF_UP);

        if (paidAmount.compareTo(totalAmount) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paid amount cannot be greater than total amount"
            );
        }

        BigDecimal dueAmount = totalAmount.subtract(paidAmount)
                .setScale(2, RoundingMode.HALF_UP);

        Purchase purchase = Purchase.builder()
                .purchaseNumber(generatePurchaseNumber())
                .supplier(supplier)
                .status("RECEIVED")
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .note(cleanOptionalText(request.getNote()))
                .createdBy(currentUser)
                .build();

        Purchase savedPurchase = purchaseRepository.saveAndFlush(purchase);

        for (PreparedPurchaseItem preparedItem : preparedItems.values()) {
            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .purchase(savedPurchase)
                    .product(preparedItem.product())
                    .productName(preparedItem.product().getName())
                    .productSku(preparedItem.product().getSku())
                    .quantity(preparedItem.quantity())
                    .unitCost(preparedItem.unitCost())
                    .lineTotal(preparedItem.lineTotal())
                    .build();

            purchaseItemRepository.save(purchaseItem);

            addStockFromPurchase(
                    preparedItem.product(),
                    preparedItem.quantity(),
                    savedPurchase.getId(),
                    currentUser
            );
        }

        auditLogService.record(
                "PURCHASE_CREATED",
                "PURCHASE",
                savedPurchase.getId(),
                "Purchase created: " + savedPurchase.getPurchaseNumber()
        );

        Purchase purchaseWithDetails = purchaseRepository.findById(savedPurchase.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Purchase was not found after creation"
                ));

        return mapToPurchaseResponse(purchaseWithDetails);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getAllPurchases() {
        return purchaseRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToPurchaseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with ID: " + id
                ));

        return mapToPurchaseResponse(purchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getPurchasesBySupplier(Long supplierId) {
        return purchaseRepository.findBySupplier_IdOrderByCreatedAtDesc(supplierId)
                .stream()
                .map(this::mapToPurchaseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getPurchasesBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        return purchaseRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)
                .stream()
                .map(this::mapToPurchaseResponse)
                .toList();
    }

    private Map<Long, PreparedPurchaseItem> preparePurchaseItems(
            List<PurchaseItemRequest> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Purchase must contain at least one item"
            );
        }

        Map<Long, PreparedPurchaseItem> preparedItems = new LinkedHashMap<>();

        for (PurchaseItemRequest item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Product not found with ID: " + item.getProductId()
                    ));

            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Inactive product cannot be purchased: " + product.getName()
                );
            }

            BigDecimal quantity = item.getQuantity()
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal unitCost = item.getUnitCost()
                    .setScale(2, RoundingMode.HALF_UP);

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Purchase quantity must be greater than zero"
                );
            }

            if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unit cost cannot be negative"
                );
            }

            PreparedPurchaseItem existing = preparedItems.get(product.getId());

            if (existing == null) {
                BigDecimal lineTotal = unitCost.multiply(quantity)
                        .setScale(2, RoundingMode.HALF_UP);

                preparedItems.put(
                        product.getId(),
                        new PreparedPurchaseItem(product, quantity, unitCost, lineTotal)
                );
            } else {
                if (existing.unitCost().compareTo(unitCost) != 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Duplicate product must use the same unit cost: " + product.getName()
                    );
                }

                BigDecimal mergedQuantity = existing.quantity().add(quantity)
                        .setScale(3, RoundingMode.HALF_UP);

                BigDecimal mergedLineTotal = unitCost.multiply(mergedQuantity)
                        .setScale(2, RoundingMode.HALF_UP);

                preparedItems.put(
                        product.getId(),
                        new PreparedPurchaseItem(product, mergedQuantity, unitCost, mergedLineTotal)
                );
            }
        }

        return preparedItems;
    }

    private void addStockFromPurchase(
            Product product,
            BigDecimal quantity,
            Long purchaseId,
            User currentUser
    ) {
        BigDecimal stockBefore = product.getCurrentStock() == null
                ? BigDecimal.ZERO
                : product.getCurrentStock();

        BigDecimal stockAfter = stockBefore.add(quantity)
                .setScale(3, RoundingMode.HALF_UP);

        product.setCurrentStock(stockAfter);
        productRepository.save(product);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(product);
        transaction.setTransactionType(InventoryTransactionType.PURCHASE);
        transaction.setQuantityChange(quantity);
        transaction.setStockBefore(stockBefore);
        transaction.setStockAfter(stockAfter);
        transaction.setReferenceType("PURCHASE");
        transaction.setReferenceId(purchaseId);
        transaction.setNote("Stock added from supplier purchase");
        transaction.setCreatedBy(currentUser);

        inventoryTransactionRepository.save(transaction);
    }

    private PurchaseResponse mapToPurchaseResponse(Purchase purchase) {
        return PurchaseResponse.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplierId(purchase.getSupplier().getId())
                .supplierName(purchase.getSupplier().getName())
                .status(purchase.getStatus())
                .subtotal(purchase.getSubtotal())
                .discountAmount(purchase.getDiscountAmount())
                .totalAmount(purchase.getTotalAmount())
                .paidAmount(purchase.getPaidAmount())
                .dueAmount(purchase.getDueAmount())
                .note(purchase.getNote())
                .createdById(purchase.getCreatedBy().getId())
                .createdByUsername(purchase.getCreatedBy().getUsername())
                .items(purchaseItemRepository.findByPurchase_IdOrderByIdAsc(purchase.getId())
                        .stream()
                        .map(this::mapToPurchaseItemResponse)
                        .toList())
                .createdAt(purchase.getCreatedAt())
                .updatedAt(purchase.getUpdatedAt())
                .build();
    }

    private PurchaseResponse.Item mapToPurchaseItemResponse(PurchaseItem item) {
        return PurchaseResponse.Item.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .unitCost(item.getUnitCost())
                .lineTotal(item.getLineTotal())
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
            throw new AccessDeniedException("Inactive user cannot create purchases");
        }

        return user;
    }

    private String generatePurchaseNumber() {
        String purchaseNumber;

        do {
            purchaseNumber = "PUR-" + System.currentTimeMillis();
        } while (purchaseRepository.existsByPurchaseNumber(purchaseNumber));

        return purchaseNumber;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String cleanOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private record PreparedPurchaseItem(
            Product product,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal
    ) {
    }
}