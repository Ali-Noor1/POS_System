package com.possystem.service;

import com.possystem.dto.InventoryAdjustmentRequest;
import com.possystem.dto.InventoryAdjustmentResponse;
import com.possystem.entity.Category;
import com.possystem.entity.InventoryTransaction;
import com.possystem.entity.InventoryTransactionType;
import com.possystem.entity.Product;
import com.possystem.entity.Role;
import com.possystem.entity.User;
import com.possystem.repository.InventoryTransactionRepository;
import com.possystem.repository.ProductRepository;
import com.possystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                productRepository,
                inventoryTransactionRepository,
                userRepository,
                auditLogService
        );
    }

    @Test
    void adjustStock_recordsAuditLogAfterSuccessfulAdjustment() {

        Product product = product();
        User admin = adminUser();

        InventoryAdjustmentRequest request =
                new InventoryAdjustmentRequest();
        request.setProductId(5L);
        request.setTransactionType(InventoryTransactionType.ADJUSTMENT_IN);
        request.setQuantity(new BigDecimal("3.000"));
        request.setNote("  Restock  ");

        when(productRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(product));
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));
        when(inventoryTransactionRepository.save(
                any(InventoryTransaction.class)
        )).thenAnswer(invocation -> {
            InventoryTransaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", 50L);
            return transaction;
        });

        InventoryAdjustmentResponse response = inventoryService.adjustStock(
                request,
                "admin"
        );

        assertEquals(50L, response.getTransactionId());
        assertEquals(new BigDecimal("8.000"), response.getStockAfter());

        verify(auditLogService).record(
                "STOCK_ADJUSTED",
                "INVENTORY_TRANSACTION",
                50L,
                "Stock adjusted for product SKU-001 by 3.000"
        );
    }

    private Product product() {
        Category category = Category.builder()
                .name("Perfumes")
                .build();
        ReflectionTestUtils.setField(category, "id", 2L);

        Product product = Product.builder()
                .category(category)
                .name("Perfume Bottle")
                .sku("SKU-001")
                .currentStock(new BigDecimal("5.000"))
                .reorderLevel(new BigDecimal("1.000"))
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(product, "id", 5L);

        return product;
    }

    private User adminUser() {
        Role role = new Role("ADMIN", "Admin role");
        User user = new User(
                "Admin User",
                "admin",
                "admin@example.com",
                "encoded-password",
                "ACTIVE",
                role
        );
        user.setId(1L);
        return user;
    }
}
