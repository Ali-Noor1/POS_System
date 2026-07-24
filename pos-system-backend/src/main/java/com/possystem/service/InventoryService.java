package com.possystem.service;

import com.possystem.dto.InventoryAdjustmentRequest;
import com.possystem.dto.InventoryAdjustmentResponse;
import com.possystem.entity.InventoryTransaction;
import com.possystem.entity.InventoryTransactionType;
import com.possystem.entity.Product;
import com.possystem.entity.User;
import com.possystem.repository.InventoryTransactionRepository;
import com.possystem.repository.ProductRepository;
import com.possystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.possystem.dto.InventoryTransactionResponse;
import com.possystem.dto.LowStockProductResponse;


import java.util.List;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public InventoryAdjustmentResponse adjustStock(
            InventoryAdjustmentRequest request,
            String username
    ) {

        Product product = productRepository
                .findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with ID: " + request.getProductId()
                ));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot adjust stock for an inactive product"
            );
        }

        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(currentUser.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Inactive user cannot adjust stock"
            );
        }

        InventoryTransactionType transactionType =
                request.getTransactionType();

        if (transactionType != InventoryTransactionType.ADJUSTMENT_IN
                && transactionType != InventoryTransactionType.ADJUSTMENT_OUT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ADJUSTMENT_IN and ADJUSTMENT_OUT are allowed here"
            );
        }

        BigDecimal stockBefore = product.getCurrentStock();

        BigDecimal quantityChange =
                transactionType == InventoryTransactionType.ADJUSTMENT_IN
                        ? request.getQuantity()
                        : request.getQuantity().negate();

        BigDecimal stockAfter = stockBefore.add(quantityChange);

        if (stockAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock cannot become negative. Current stock is: "
                            + stockBefore
            );
        }

        product.setCurrentStock(stockAfter);

        InventoryTransaction inventoryTransaction =
                new InventoryTransaction();

        inventoryTransaction.setProduct(product);
        inventoryTransaction.setTransactionType(transactionType);
        inventoryTransaction.setQuantityChange(quantityChange);
        inventoryTransaction.setStockBefore(stockBefore);
        inventoryTransaction.setStockAfter(stockAfter);
        inventoryTransaction.setReferenceType("MANUAL_ADJUSTMENT");
        inventoryTransaction.setReferenceId(null);
        inventoryTransaction.setNote(cleanOptionalText(request.getNote()));
        inventoryTransaction.setCreatedBy(currentUser);

        productRepository.save(product);

        InventoryTransaction savedTransaction =
                inventoryTransactionRepository.save(inventoryTransaction);

        auditLogService.record(
                "STOCK_ADJUSTED",
                "INVENTORY_TRANSACTION",
                savedTransaction.getId(),
                "Stock adjusted for product "
                        + product.getSku()
                        + " by "
                        + quantityChange
        );

        return mapToInventoryAdjustmentResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionResponse> getProductTransactions(
            Long productId
    ) {
        /*
         * First confirm product exists.
         * This gives a clean 404 instead of returning an empty list
         * for a product ID that does not exist.
         */
        productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with ID: " + productId
                ));

        return inventoryTransactionRepository
                .findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToInventoryTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LowStockProductResponse> getLowStockProducts() {

        return productRepository
                .findActiveLowStockProducts()
                .stream()
                .map(this::mapToLowStockProductResponse)
                .toList();
    }



    private InventoryAdjustmentResponse mapToInventoryAdjustmentResponse(
            InventoryTransaction transaction
    ) {
        return InventoryAdjustmentResponse.builder()
                .transactionId(transaction.getId())
                .productId(transaction.getProduct().getId())
                .productName(transaction.getProduct().getName())
                .transactionType(transaction.getTransactionType())
                .quantityChange(transaction.getQuantityChange())
                .stockBefore(transaction.getStockBefore())
                .stockAfter(transaction.getStockAfter())
                .note(transaction.getNote())
                .createdByUsername(transaction.getCreatedBy().getUsername())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private InventoryTransactionResponse mapToInventoryTransactionResponse(
            InventoryTransaction transaction
    ) {
        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .productId(transaction.getProduct().getId())
                .productName(transaction.getProduct().getName())
                .transactionType(transaction.getTransactionType())
                .quantityChange(transaction.getQuantityChange())
                .stockBefore(transaction.getStockBefore())
                .stockAfter(transaction.getStockAfter())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .note(transaction.getNote())
                .createdByUserId(transaction.getCreatedBy().getId())
                .createdByUsername(transaction.getCreatedBy().getUsername())
                .createdByFullName(transaction.getCreatedBy().getFullName())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
    private String cleanOptionalText(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private LowStockProductResponse mapToLowStockProductResponse(
            Product product
    ) {
        BigDecimal shortageQuantity = product.getReorderLevel()
                .subtract(product.getCurrentStock());

        return LowStockProductResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .currentStock(product.getCurrentStock())
                .reorderLevel(product.getReorderLevel())
                .shortageQuantity(shortageQuantity)
                .build();
    }
}
