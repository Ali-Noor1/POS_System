package com.possystem.service;

import com.possystem.dto.*;
import com.possystem.entity.*;
import com.possystem.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReceiptNumberService receiptNumberService;

    @Mock
    private AuditLogService auditLogService;

    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(
                userRepository,
                productRepository,
                customerRepository,
                inventoryTransactionRepository,
                saleRepository,
                saleItemRepository,
                paymentRepository,
                receiptNumberService,
                auditLogService
        );
    }
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // AUTHENTICATED SALE USER TESTS
    // =========================================================

    @Test
    void getAuthenticatedSaleUser_returnsActiveLoggedInUser() {

        User activeCashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(activeCashier));

        authenticateAsCashier("cashier");

        User result = saleService.getAuthenticatedSaleUser();

        assertSame(activeCashier, result);
        verify(userRepository).findByUsername("cashier");
    }

    @Test
    void getAuthenticatedSaleUser_throwsException_whenUserIsInactive() {

        User inactiveCashier = createUser(
                2L,
                "cashier",
                "INACTIVE"
        );

        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(inactiveCashier));

        authenticateAsCashier("cashier");

        assertThrows(
                AccessDeniedException.class,
                () -> saleService.getAuthenticatedSaleUser()
        );
    }

    // =========================================================
    // PRODUCT LOADING AND STATUS TESTS
    // =========================================================

    @Test
    void loadActiveProductForSale_returnsLockedActiveProduct() {

        Product activeProduct = createProduct(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000"
        );

        when(productRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(activeProduct));

        Product result = saleService.loadActiveProductForSale(5L);

        assertSame(activeProduct, result);
        verify(productRepository).findByIdForUpdate(5L);
    }

    @Test
    void loadActiveProductForSale_throwsNotFound_whenProductDoesNotExist() {

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.loadActiveProductForSale(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Product not found", exception.getReason());
    }

    @Test
    void loadActiveProductForSale_throwsConflict_whenProductIsInactive() {

        Product inactiveProduct = createProduct(
                7L,
                "Old Product",
                "OLD-001",
                "INACTIVE",
                "10.000"
        );

        when(productRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(inactiveProduct));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.loadActiveProductForSale(7L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Inactive product cannot be sold",
                exception.getReason()
        );
    }

    // =========================================================
    // QUANTITY AND STOCK VALIDATION TESTS
    // =========================================================

    @Test
    void validateQuantityAndStockForSale_doesNotThrow_whenQuantityIsAvailable() {

        Product product = createProduct(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000"
        );

        saleService.validateQuantityAndStockForSale(
                product,
                new BigDecimal("3.000")
        );
    }

    @Test
    void validateQuantityAndStockForSale_throwsBadRequest_whenQuantityIsZero() {

        Product product = createProduct(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.validateQuantityAndStockForSale(
                        product,
                        BigDecimal.ZERO
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Sale quantity must be greater than zero",
                exception.getReason()
        );
    }

    @Test
    void validateQuantityAndStockForSale_throwsConflict_whenStockIsInsufficient() {

        Product product = createProduct(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "2.000"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.validateQuantityAndStockForSale(
                        product,
                        new BigDecimal("3.000")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Insufficient stock for product: Dior Sauvage 100ml",
                exception.getReason()
        );
    }

    // =========================================================
    // CUSTOMER VALIDATION TESTS
    // =========================================================

    @Test
    void loadActiveCustomerForSale_returnsNull_whenCustomerIdIsNull() {

        Customer result = saleService.loadActiveCustomerForSale(null);

        assertNull(result);
    }

    @Test
    void loadActiveCustomerForSale_returnsCustomer_whenCustomerIsActive() {

        Customer activeCustomer = createCustomer(
                3L,
                "Ali Khan",
                "+923001234567",
                "ACTIVE"
        );

        when(customerRepository.findById(3L))
                .thenReturn(Optional.of(activeCustomer));

        Customer result = saleService.loadActiveCustomerForSale(3L);

        assertSame(activeCustomer, result);
        verify(customerRepository).findById(3L);
    }

    @Test
    void loadActiveCustomerForSale_throwsNotFound_whenCustomerDoesNotExist() {

        when(customerRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.loadActiveCustomerForSale(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Customer not found", exception.getReason());
    }

    @Test
    void loadActiveCustomerForSale_throwsConflict_whenCustomerIsInactive() {

        Customer inactiveCustomer = createCustomer(
                4L,
                "Old Customer",
                "+923009999999",
                "INACTIVE"
        );

        when(customerRepository.findById(4L))
                .thenReturn(Optional.of(inactiveCustomer));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.loadActiveCustomerForSale(4L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Inactive customer cannot be selected for a sale",
                exception.getReason()
        );
    }


// =========================================================
// PAYMENT VALIDATION AND CHANGE CALCULATION TESTS
// =========================================================

    @Test
    void calculateChangeAmount_returnsCorrectChange_forCashPayment() {

        BigDecimal changeAmount = saleService.calculateChangeAmount(
                PaymentMethod.CASH,
                new BigDecimal("5000.00"),
                new BigDecimal("6000.00")
        );

        assertEquals(
                new BigDecimal("1000.00"),
                changeAmount
        );
    }

    @Test
    void calculateChangeAmount_returnsZero_forCardPaymentWithExactAmount() {

        BigDecimal changeAmount = saleService.calculateChangeAmount(
                PaymentMethod.CARD,
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00")
        );

        assertEquals(
                new BigDecimal("0.00"),
                changeAmount
        );
    }

    @Test
    void calculateChangeAmount_throwsBadRequest_whenPaymentMethodIsMissing() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.calculateChangeAmount(
                        null,
                        new BigDecimal("5000.00"),
                        new BigDecimal("5000.00")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Payment method is required",
                exception.getReason()
        );
    }

    @Test
    void calculateChangeAmount_throwsBadRequest_whenSaleTotalIsZero() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.calculateChangeAmount(
                        PaymentMethod.CASH,
                        BigDecimal.ZERO,
                        new BigDecimal("5000.00")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Sale total must be greater than zero",
                exception.getReason()
        );
    }

    @Test
    void calculateChangeAmount_throwsBadRequest_whenAmountReceivedIsMissing() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.calculateChangeAmount(
                        PaymentMethod.CASH,
                        new BigDecimal("5000.00"),
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Amount received must be greater than zero",
                exception.getReason()
        );
    }

    @Test
    void calculateChangeAmount_throwsConflict_whenCashIsLessThanSaleTotal() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.calculateChangeAmount(
                        PaymentMethod.CASH,
                        new BigDecimal("5000.00"),
                        new BigDecimal("4000.00")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Cash received is less than the sale total",
                exception.getReason()
        );
    }

    @Test
    void calculateChangeAmount_throwsConflict_whenCardAmountDoesNotMatchSaleTotal() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.calculateChangeAmount(
                        PaymentMethod.CARD,
                        new BigDecimal("5000.00"),
                        new BigDecimal("6000.00")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Payment amount must exactly match the sale total",
                exception.getReason()
        );
    }

    // =========================================================
// CART ITEM MERGING TESTS
// =========================================================

    @Test
    void mergeSaleItemQuantities_combinesDuplicateProducts() {

        CompleteSaleItemRequest firstItem = createSaleItem(
                5L,
                "1.000"
        );

        CompleteSaleItemRequest secondItem = createSaleItem(
                5L,
                "2.000"
        );

        CompleteSaleItemRequest thirdItem = createSaleItem(
                7L,
                "1.000"
        );

        Map<Long, BigDecimal> result =
                saleService.mergeSaleItemQuantities(
                        List.of(firstItem, secondItem, thirdItem)
                );

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("3.000"), result.get(5L));
        assertEquals(new BigDecimal("1.000"), result.get(7L));
    }

    @Test
    void mergeSaleItemQuantities_keepsDifferentProductsSeparate() {

        CompleteSaleItemRequest firstItem = createSaleItem(
                5L,
                "2.000"
        );

        CompleteSaleItemRequest secondItem = createSaleItem(
                7L,
                "3.000"
        );

        Map<Long, BigDecimal> result =
                saleService.mergeSaleItemQuantities(
                        List.of(firstItem, secondItem)
                );

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("2.000"), result.get(5L));
        assertEquals(new BigDecimal("3.000"), result.get(7L));
    }

    @Test
    void mergeSaleItemQuantities_throwsBadRequest_whenItemListIsEmpty() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.mergeSaleItemQuantities(List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "At least one sale item is required",
                exception.getReason()
        );
    }

    @Test
    void mergeSaleItemQuantities_throwsBadRequest_whenProductIdIsInvalid() {

        CompleteSaleItemRequest item = createSaleItem(
                0L,
                "1.000"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.mergeSaleItemQuantities(List.of(item))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Product ID must be greater than zero",
                exception.getReason()
        );
    }

    @Test
    void mergeSaleItemQuantities_throwsBadRequest_whenQuantityIsInvalid() {

        CompleteSaleItemRequest item = createSaleItem(
                5L,
                "0.000"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.mergeSaleItemQuantities(List.of(item))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Sale quantity must be greater than zero",
                exception.getReason()
        );
    }

    // =========================================================
// SALE PRICE AND LINE TOTAL TESTS
// =========================================================

    @Test
    void calculateSaleLineTotal_returnsCorrectTotal_usingProductSellingPrice() {

        Product product = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000",
                "12000.00"
        );

        BigDecimal lineTotal = saleService.calculateSaleLineTotal(
                product,
                new BigDecimal("2.000")
        );

        assertEquals(
                new BigDecimal("24000.00"),
                lineTotal
        );
    }

    @Test
    void calculateSaleLineTotal_roundsMoneyToTwoDecimalPlaces() {

        Product product = createProductWithPrice(
                5L,
                "Test Product",
                "TEST-001",
                "ACTIVE",
                "10.000",
                "299.99"
        );

        BigDecimal lineTotal = saleService.calculateSaleLineTotal(
                product,
                new BigDecimal("1.500")
        );

        /*
         * 299.99 × 1.500 = 449.985
         * Rounded HALF_UP to 2 decimal places = 449.99
         */
        assertEquals(
                new BigDecimal("449.99"),
                lineTotal
        );
    }

    @Test
    void calculateSaleLineTotal_throwsConflict_whenSellingPriceIsZero() {

        Product product = createProductWithPrice(
                5L,
                "Free Product",
                "FREE-001",
                "ACTIVE",
                "10.000",
                "0.00"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.calculateSaleLineTotal(
                        product,
                        new BigDecimal("1.000")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Product selling price must be greater than zero",
                exception.getReason()
        );
    }

    // =========================================================
// SALE STOCK REDUCTION AND INVENTORY HISTORY TESTS
// =========================================================

    @Test
    void reduceStockAndCreateSaleInventoryTransaction_reducesStockAndCreatesSaleHistory() {

        Product product = createProduct(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000"
        );

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        saleService.reduceStockAndCreateSaleInventoryTransaction(
                product,
                new BigDecimal("3.000"),
                12L,
                cashier
        );

        assertEquals(
                new BigDecimal("7.000"),
                product.getCurrentStock()
        );

        verify(productRepository).save(product);

        ArgumentCaptor<InventoryTransaction> transactionCaptor =
                ArgumentCaptor.forClass(InventoryTransaction.class);

        verify(inventoryTransactionRepository)
                .save(transactionCaptor.capture());

        InventoryTransaction transaction =
                transactionCaptor.getValue();

        assertSame(product, transaction.getProduct());
        assertEquals(
                InventoryTransactionType.SALE,
                transaction.getTransactionType()
        );
        assertEquals(
                new BigDecimal("-3.000"),
                transaction.getQuantityChange()
        );
        assertEquals(
                new BigDecimal("10.000"),
                transaction.getStockBefore()
        );
        assertEquals(
                new BigDecimal("7.000"),
                transaction.getStockAfter()
        );
        assertEquals("SALE", transaction.getReferenceType());
        assertEquals(12L, transaction.getReferenceId());
        assertEquals(
                "Stock reduced after completed sale",
                transaction.getNote()
        );
        assertSame(cashier, transaction.getCreatedBy());
    }

    // =========================================================
// SALE CREATION TESTS
// =========================================================

    @Test
    void createCompletedSale_createsAndSavesCompletedSale() {

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        Customer customer = createCustomer(
                3L,
                "Ali Khan",
                "+923001234567",
                "ACTIVE"
        );

        when(receiptNumberService.generateUniqueReceiptNumber())
                .thenReturn("REC-20260702-A1B2C3D4");

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Sale savedSale = saleService.createCompletedSale(
                customer,
                cashier,
                new BigDecimal("24000.00"),
                BigDecimal.ZERO,
                new BigDecimal("24000.00")
        );

        assertEquals(
                "REC-20260702-A1B2C3D4",
                savedSale.getReceiptNumber()
        );

        assertSame(customer, savedSale.getCustomer());
        assertSame(cashier, savedSale.getCashier());

        assertEquals(
                SaleStatus.COMPLETED,
                savedSale.getStatus()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                savedSale.getSubtotal()
        );

        assertEquals(
                BigDecimal.ZERO,
                savedSale.getDiscountAmount()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                savedSale.getTotalAmount()
        );

        ArgumentCaptor<Sale> saleCaptor =
                ArgumentCaptor.forClass(Sale.class);

        verify(saleRepository).save(saleCaptor.capture());

        Sale saleSentForSave = saleCaptor.getValue();

        assertEquals(
                "REC-20260702-A1B2C3D4",
                saleSentForSave.getReceiptNumber()
        );

        assertSame(customer, saleSentForSave.getCustomer());
        assertSame(cashier, saleSentForSave.getCashier());

        verify(receiptNumberService, times(1))
                .generateUniqueReceiptNumber();
    }

    @Test
    void createCompletedSale_allowsWalkInCustomer() {

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        when(receiptNumberService.generateUniqueReceiptNumber())
                .thenReturn("REC-20260702-Z9Y8X7W6");

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Sale savedSale = saleService.createCompletedSale(
                null,
                cashier,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00")
        );

        assertEquals(
                "REC-20260702-Z9Y8X7W6",
                savedSale.getReceiptNumber()
        );

        assertNull(savedSale.getCustomer());

        assertSame(cashier, savedSale.getCashier());

        assertEquals(
                SaleStatus.COMPLETED,
                savedSale.getStatus()
        );
    }

    // =========================================================
// SALE ITEM CREATION TESTS
// =========================================================

    @Test
    void createSaleItem_createsAndSavesProductSnapshot() {

        Sale sale = new Sale();

        Product product = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000",
                "12000.00"
        );

        when(saleItemRepository.save(any(SaleItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleItem savedSaleItem = saleService.createSaleItem(
                sale,
                product,
                new BigDecimal("2.000")
        );

        assertSame(sale, savedSaleItem.getSale());
        assertSame(product, savedSaleItem.getProduct());

        assertEquals(
                "Dior Sauvage 100ml",
                savedSaleItem.getProductName()
        );

        assertEquals(
                "PERF-001",
                savedSaleItem.getProductSku()
        );

        assertEquals(
                new BigDecimal("2.000"),
                savedSaleItem.getQuantity()
        );

        assertEquals(
                new BigDecimal("12000.00"),
                savedSaleItem.getUnitPrice()
        );

        assertEquals(
                new BigDecimal("0.00"),
                savedSaleItem.getDiscountAmount()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                savedSaleItem.getLineTotal()
        );

        ArgumentCaptor<SaleItem> saleItemCaptor =
                ArgumentCaptor.forClass(SaleItem.class);

        verify(saleItemRepository).save(saleItemCaptor.capture());

        SaleItem itemSentForSave = saleItemCaptor.getValue();

        assertSame(sale, itemSentForSave.getSale());
        assertSame(product, itemSentForSave.getProduct());
    }

    @Test
    void createSaleItem_throwsBadRequest_whenSaleIsMissing() {

        Product product = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000",
                "12000.00"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.createSaleItem(
                        null,
                        product,
                        new BigDecimal("1.000")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Sale is required",
                exception.getReason()
        );
    }

    // =========================================================
// PAYMENT CREATION TESTS
// =========================================================

    @Test
    void createPaidPayment_createsCashPaymentWithCorrectChange() {

        Sale sale = createSale(
                "REC-20260702-A1B2C3D4",
                "5000.00"
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment savedPayment = saleService.createPaidPayment(
                sale,
                PaymentMethod.CASH,
                new BigDecimal("6000.00"),
                null
        );

        assertSame(sale, savedPayment.getSale());

        assertEquals(
                PaymentMethod.CASH,
                savedPayment.getPaymentMethod()
        );

        assertEquals(
                PaymentStatus.PAID,
                savedPayment.getPaymentStatus()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                savedPayment.getAmount()
        );

        assertEquals(
                new BigDecimal("6000.00"),
                savedPayment.getAmountReceived()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                savedPayment.getChangeAmount()
        );

        assertNull(savedPayment.getReferenceNumber());

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(paymentCaptor.capture());

        Payment paymentSentForSave = paymentCaptor.getValue();

        assertSame(sale, paymentSentForSave.getSale());

        assertEquals(
                PaymentStatus.PAID,
                paymentSentForSave.getPaymentStatus()
        );
    }

    @Test
    void createPaidPayment_createsCardPaymentWithZeroChange() {

        Sale sale = createSale(
                "REC-20260702-Z9Y8X7W6",
                "5000.00"
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment savedPayment = saleService.createPaidPayment(
                sale,
                PaymentMethod.CARD,
                new BigDecimal("5000.00"),
                " CARD-TXN-12345 "
        );

        assertSame(sale, savedPayment.getSale());

        assertEquals(
                PaymentMethod.CARD,
                savedPayment.getPaymentMethod()
        );

        assertEquals(
                PaymentStatus.PAID,
                savedPayment.getPaymentStatus()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                savedPayment.getAmount()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                savedPayment.getAmountReceived()
        );

        assertEquals(
                new BigDecimal("0.00"),
                savedPayment.getChangeAmount()
        );

        assertEquals(
                "CARD-TXN-12345",
                savedPayment.getReferenceNumber()
        );
    }

    @Test
    void createPaidPayment_throwsBadRequest_whenSaleIsMissing() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.createPaidPayment(
                        null,
                        PaymentMethod.CASH,
                        new BigDecimal("5000.00"),
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Sale is required",
                exception.getReason()
        );
    }


    // =========================================================
// COMPLETE SALE TRANSACTION TESTS
// =========================================================

    @Test
    void completeSale_createsReceiptItemsPaymentAndInventoryHistory() {

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        Product product = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "10.000",
                "12000.00"
        );

        authenticateAsCashier("cashier");

        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(cashier));

        /*
         * Same product is scanned twice.
         * Backend must merge 1.000 + 1.000 into 2.000.
         */
        when(productRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(product));

        when(receiptNumberService.generateUniqueReceiptNumber())
                .thenReturn("REC-20260702-A1B2C3D4");

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale savedSale = invocation.getArgument(0);

                    /*
                     * Simulate Hibernate/MySQL generated ID.
                     */
                    ReflectionTestUtils.setField(
                            savedSale,
                            "id",
                            100L
                    );

                    return savedSale;
                });

        when(saleItemRepository.save(any(SaleItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompleteSaleRequest request = new CompleteSaleRequest();

        request.setItems(List.of(
                createSaleItem(5L, "1.000"),
                createSaleItem(5L, "1.000")
        ));

        CompleteSalePaymentRequest paymentRequest =
                new CompleteSalePaymentRequest();

        paymentRequest.setPaymentMethod(PaymentMethod.CASH);
        paymentRequest.setAmountReceived(
                new BigDecimal("25000.00")
        );
        paymentRequest.setReferenceNumber(null);

        request.setPayment(paymentRequest);

        /*
         * customerId stays null.
         * This means walk-in customer sale.
         */
        CompleteSaleResponse response =
                saleService.completeSale(request);

        assertEquals(100L, response.getSaleId());

        assertEquals(
                "REC-20260702-A1B2C3D4",
                response.getReceiptNumber()
        );

        assertNull(response.getCustomerId());
        assertNull(response.getCustomerName());

        assertEquals(2L, response.getCashierId());
        assertEquals("cashier", response.getCashierUsername());

        assertEquals(
                SaleStatus.COMPLETED,
                response.getSaleStatus()
        );

        /*
         * 12000 × 2 = 24000
         */
        assertEquals(
                new BigDecimal("24000.00"),
                response.getSubtotal()
        );

        assertEquals(
                new BigDecimal("0.00"),
                response.getDiscountAmount()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                response.getTotalAmount()
        );

        assertEquals(
                new BigDecimal("25000.00"),
                response.getAmountReceived()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                response.getChangeAmount()
        );

        assertEquals(1, response.getItems().size());

        assertEquals(
                "Dior Sauvage 100ml",
                response.getItems().get(0).getProductName()
        );

        assertEquals(
                new BigDecimal("2.000"),
                response.getItems().get(0).getQuantity()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                response.getItems().get(0).getLineTotal()
        );

        assertEquals(
                PaymentMethod.CASH,
                response.getPayment().getPaymentMethod()
        );

        assertEquals(
                PaymentStatus.PAID,
                response.getPayment().getPaymentStatus()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                response.getPayment().getAmount()
        );

        /*
         * Original stock was 10.000.
         * Sold quantity was 2.000.
         */
        assertEquals(
                new BigDecimal("8.000"),
                product.getCurrentStock()
        );

        /*
         * Because duplicate product IDs were merged,
         * the product is locked only one time.
         */
        verify(productRepository, times(1))
                .findByIdForUpdate(5L);

        verify(saleRepository, times(1))
                .save(any(Sale.class));

        verify(saleItemRepository, times(1))
                .save(any(SaleItem.class));

        verify(paymentRepository, times(1))
                .save(any(Payment.class));

        verify(productRepository, times(1))
                .save(product);

        ArgumentCaptor<InventoryTransaction> inventoryCaptor =
                ArgumentCaptor.forClass(InventoryTransaction.class);

        verify(inventoryTransactionRepository, times(1))
                .save(inventoryCaptor.capture());

        InventoryTransaction inventoryTransaction =
                inventoryCaptor.getValue();

        assertEquals(
                InventoryTransactionType.SALE,
                inventoryTransaction.getTransactionType()
        );

        assertEquals(
                new BigDecimal("-2.000"),
                inventoryTransaction.getQuantityChange()
        );

        assertEquals(
                new BigDecimal("10.000"),
                inventoryTransaction.getStockBefore()
        );

        assertEquals(
                new BigDecimal("8.000"),
                inventoryTransaction.getStockAfter()
        );

        assertEquals(
                "SALE",
                inventoryTransaction.getReferenceType()
        );

        assertEquals(
                100L,
                inventoryTransaction.getReferenceId()
        );

        assertSame(cashier, inventoryTransaction.getCreatedBy());
    }

    // =========================================================
// SALE DETAIL / RECEIPT VIEW TESTS
// =========================================================

    @Test
    void getSaleDetails_returnsReceiptForOwnCashierSale() {

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        Sale sale = createSale(
                "REC-20260702-A1B2C3D4",
                "24000.00"
        );

        ReflectionTestUtils.setField(
                sale,
                "id",
                100L
        );

        sale.setCashier(cashier);

        Product product = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "8.000",
                "12000.00"
        );

        SaleItem saleItem = new SaleItem();

        saleItem.setSale(sale);
        saleItem.setProduct(product);
        saleItem.setProductName("Dior Sauvage 100ml");
        saleItem.setProductSku("PERF-001");
        saleItem.setQuantity(new BigDecimal("2.000"));
        saleItem.setUnitPrice(new BigDecimal("12000.00"));
        saleItem.setDiscountAmount(new BigDecimal("0.00"));
        saleItem.setLineTotal(new BigDecimal("24000.00"));

        Payment payment = new Payment();

        payment.setSale(sale);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setAmount(new BigDecimal("24000.00"));
        payment.setAmountReceived(new BigDecimal("25000.00"));
        payment.setChangeAmount(new BigDecimal("1000.00"));

        authenticateAsCashier("cashier");

        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(cashier));

        when(saleRepository.findById(100L))
                .thenReturn(Optional.of(sale));

        when(saleItemRepository.findBySaleIdOrderByIdAsc(100L))
                .thenReturn(List.of(saleItem));

        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(payment));

        CompleteSaleResponse response =
                saleService.getSaleDetails(100L);

        assertEquals(100L, response.getSaleId());

        assertEquals(
                "REC-20260702-A1B2C3D4",
                response.getReceiptNumber()
        );

        assertEquals(2L, response.getCashierId());

        assertEquals(
                "cashier",
                response.getCashierUsername()
        );

        assertEquals(
                SaleStatus.COMPLETED,
                response.getSaleStatus()
        );

        assertEquals(
                new BigDecimal("24000.00"),
                response.getTotalAmount()
        );

        assertEquals(
                new BigDecimal("25000.00"),
                response.getAmountReceived()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                response.getChangeAmount()
        );

        assertEquals(1, response.getItems().size());

        assertEquals(
                "Dior Sauvage 100ml",
                response.getItems().get(0).getProductName()
        );

        assertEquals(
                new BigDecimal("2.000"),
                response.getItems().get(0).getQuantity()
        );

        assertEquals(
                PaymentMethod.CASH,
                response.getPayment().getPaymentMethod()
        );

        verify(saleRepository).findById(100L);

        verify(saleItemRepository)
                .findBySaleIdOrderByIdAsc(100L);

        verify(paymentRepository)
                .findBySaleIdOrderByCreatedAtAsc(100L);
    }

    @Test
    void getSaleDetails_blocksCashierFromViewingAnotherCashiersSale() {

        User loggedInCashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        User differentCashier = createUser(
                3L,
                "other_cashier",
                "ACTIVE"
        );

        Sale sale = createSale(
                "REC-20260702-Z9Y8X7W6",
                "5000.00"
        );

        ReflectionTestUtils.setField(
                sale,
                "id",
                101L
        );

        sale.setCashier(differentCashier);

        authenticateAsCashier("cashier");

        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(loggedInCashier));

        when(saleRepository.findById(101L))
                .thenReturn(Optional.of(sale));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> saleService.getSaleDetails(101L)
        );

        assertEquals(
                "Cashier can only view their own sales",
                exception.getMessage()
        );

        verifyNoInteractions(
                saleItemRepository,
                paymentRepository
        );
    }

    // =========================================================
    // SALES HISTORY TESTS
    // =========================================================

    @Test
    void getSalesHistory_returnsOnlyOwnSalesForCashier() {

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        Customer customer = createCustomer(
                3L,
                "Ali Khan",
                "+923001234567",
                "ACTIVE"
        );

        Sale newestSale = createSaleForHistory(
                100L,
                "REC-20260702-NEW001",
                "24000.00",
                cashier,
                customer
        );

        Sale olderSale = createSaleForHistory(
                99L,
                "REC-20260702-OLD001",
                "5000.00",
                cashier,
                null
        );

        Payment newestPayment = createPaymentForHistory(
                newestSale,
                PaymentMethod.CASH
        );

        Payment olderPayment = createPaymentForHistory(
                olderSale,
                PaymentMethod.CARD
        );

        authenticateAsCashier("cashier");

        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(cashier));

        when(saleRepository.findByCashier_IdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(newestSale, olderSale));

        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(newestPayment));

        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(99L))
                .thenReturn(List.of(olderPayment));

        List<SaleHistoryResponse> response =
                saleService.getSalesHistory();

        assertEquals(2, response.size());

        SaleHistoryResponse firstSale = response.get(0);

        assertEquals(100L, firstSale.getSaleId());
        assertEquals(
                "REC-20260702-NEW001",
                firstSale.getReceiptNumber()
        );

        assertEquals(3L, firstSale.getCustomerId());
        assertEquals("Ali Khan", firstSale.getCustomerName());

        assertEquals(2L, firstSale.getCashierId());
        assertEquals("cashier", firstSale.getCashierUsername());

        assertEquals(SaleStatus.COMPLETED, firstSale.getSaleStatus());

        assertEquals(
                new BigDecimal("24000.00"),
                firstSale.getTotalAmount()
        );

        assertEquals(
                PaymentMethod.CASH,
                firstSale.getPaymentMethod()
        );

        SaleHistoryResponse secondSale = response.get(1);

        assertEquals(99L, secondSale.getSaleId());
        assertNull(secondSale.getCustomerId());
        assertNull(secondSale.getCustomerName());

        assertEquals(
                PaymentMethod.CARD,
                secondSale.getPaymentMethod()
        );

        verify(saleRepository)
                .findByCashier_IdOrderByCreatedAtDesc(2L);

        verify(saleRepository, never())
                .findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getSalesHistory_returnsAllSalesForAdmin() {

        User admin = createUser(
                1L,
                "admin",
                "ACTIVE"
        );

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        Sale sale = createSaleForHistory(
                100L,
                "REC-20260702-ADMIN001",
                "5000.00",
                cashier,
                null
        );

        Payment payment = createPaymentForHistory(
                sale,
                PaymentMethod.MOBILE_WALLET
        );

        authenticateAsAdmin("admin");

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        when(saleRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(sale));

        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(payment));

        List<SaleHistoryResponse> response =
                saleService.getSalesHistory();

        assertEquals(1, response.size());

        assertEquals(100L, response.get(0).getSaleId());

        assertEquals(
                PaymentMethod.MOBILE_WALLET,
                response.get(0).getPaymentMethod()
        );

        verify(saleRepository)
                .findAllByOrderByCreatedAtDesc();

        verify(saleRepository, never())
                .findByCashier_IdOrderByCreatedAtDesc(anyLong());
    }

    // =========================================================
    // SALE CANCELLATION VALIDATION TESTS
    // =========================================================

    @Test
    void loadCompletedSaleForCancellation_returnsCompletedSale() {

        Sale sale = createSale(
                "REC-20260704-CANCEL01",
                "5000.00"
        );

        ReflectionTestUtils.setField(
                sale,
                "id",
                100L
        );

        when(saleRepository.findById(100L))
                .thenReturn(Optional.of(sale));

        Sale result = saleService.loadCompletedSaleForCancellation(100L);

        assertSame(sale, result);
        verify(saleRepository).findById(100L);
    }

    @Test
    void loadCompletedSaleForCancellation_throwsNotFound_whenSaleDoesNotExist() {

        when(saleRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.loadCompletedSaleForCancellation(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Sale not found", exception.getReason());
    }

    @Test
    void loadCompletedSaleForCancellation_throwsConflict_whenSaleIsAlreadyCancelled() {

        Sale sale = createSale(
                "REC-20260704-CANCELLED",
                "5000.00"
        );

        sale.setStatus(SaleStatus.CANCELLED);

        ReflectionTestUtils.setField(
                sale,
                "id",
                101L
        );

        when(saleRepository.findById(101L))
                .thenReturn(Optional.of(sale));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.loadCompletedSaleForCancellation(101L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Only completed sales can be cancelled",
                exception.getReason()
        );
    }
    @Test
    void validateCancellationReason_returnsTrimmedReason() {

        String result = saleService.validateCancellationReason(
                "  Customer returned items before leaving  "
        );

        assertEquals(
                "Customer returned items before leaving",
                result
        );
    }

    @Test
    void validateCancellationReason_throwsBadRequest_whenReasonIsBlank() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.validateCancellationReason("   ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Cancellation reason is required",
                exception.getReason()
        );
    }

    @Test
    void validateCancellationReason_throwsBadRequest_whenReasonIsTooLong() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> saleService.validateCancellationReason("a".repeat(501))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Cancellation reason must not exceed 500 characters",
                exception.getReason()
        );
    }
    @Test
    void restoreStockAndCreateSaleCancellationInventoryTransaction_restoresStockAndCreatesHistory() {

        User admin = createUser(
                1L,
                "admin",
                "ACTIVE"
        );

        Product saleItemProduct = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "0.000",
                "12000.00"
        );

        Product lockedProduct = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "8.000",
                "12000.00"
        );

        SaleItem saleItem = new SaleItem();
        saleItem.setProduct(saleItemProduct);
        saleItem.setQuantity(new BigDecimal("2.000"));

        when(productRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(lockedProduct));

        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransaction inventoryTransaction =
                saleService.restoreStockAndCreateSaleCancellationInventoryTransaction(
                        saleItem,
                        100L,
                        admin
                );

        assertEquals(
                new BigDecimal("10.000"),
                lockedProduct.getCurrentStock()
        );

        verify(productRepository).findByIdForUpdate(5L);
        verify(productRepository).save(lockedProduct);

        ArgumentCaptor<InventoryTransaction> inventoryCaptor =
                ArgumentCaptor.forClass(InventoryTransaction.class);

        verify(inventoryTransactionRepository)
                .save(inventoryCaptor.capture());

        InventoryTransaction savedTransaction =
                inventoryCaptor.getValue();

        assertSame(savedTransaction, inventoryTransaction);

        assertSame(lockedProduct, savedTransaction.getProduct());
        assertEquals(
                InventoryTransactionType.SALE_CANCELLED,
                savedTransaction.getTransactionType()
        );

        assertEquals(
                new BigDecimal("2.000"),
                savedTransaction.getQuantityChange()
        );

        assertEquals(
                new BigDecimal("8.000"),
                savedTransaction.getStockBefore()
        );

        assertEquals(
                new BigDecimal("10.000"),
                savedTransaction.getStockAfter()
        );

        assertEquals("SALE", savedTransaction.getReferenceType());
        assertEquals(100L, savedTransaction.getReferenceId());
        assertSame(admin, savedTransaction.getCreatedBy());
    }
    @Test
    void cancelSale_cancelsSaleRestoresStockRefundsPaymentAndReturnsReceipt() {

        User admin = createUser(
                1L,
                "admin",
                "ACTIVE"
        );

        User cashier = createUser(
                2L,
                "cashier",
                "ACTIVE"
        );

        Sale sale = createSale(
                "REC-20260704-CANCEL02",
                "24000.00"
        );

        ReflectionTestUtils.setField(
                sale,
                "id",
                100L
        );

        sale.setCashier(cashier);

        Product saleItemProduct = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "0.000",
                "12000.00"
        );

        Product lockedProduct = createProductWithPrice(
                5L,
                "Dior Sauvage 100ml",
                "PERF-001",
                "ACTIVE",
                "8.000",
                "12000.00"
        );

        SaleItem saleItem = new SaleItem();
        saleItem.setSale(sale);
        saleItem.setProduct(saleItemProduct);
        saleItem.setProductName("Dior Sauvage 100ml");
        saleItem.setProductSku("PERF-001");
        saleItem.setQuantity(new BigDecimal("2.000"));
        saleItem.setUnitPrice(new BigDecimal("12000.00"));
        saleItem.setDiscountAmount(new BigDecimal("0.00"));
        saleItem.setLineTotal(new BigDecimal("24000.00"));

        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setAmount(new BigDecimal("24000.00"));
        payment.setAmountReceived(new BigDecimal("25000.00"));
        payment.setChangeAmount(new BigDecimal("1000.00"));

        CancelSaleRequest request = new CancelSaleRequest();
        request.setCancellationReason("  Customer changed their mind  ");

        authenticateAsAdmin("admin");

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        when(saleRepository.findById(100L))
                .thenReturn(Optional.of(sale));

        when(saleItemRepository.findBySaleIdOrderByIdAsc(100L))
                .thenReturn(List.of(saleItem));

        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(payment));

        when(productRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(lockedProduct));

        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompleteSaleResponse response = saleService.cancelSale(
                100L,
                request
        );

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        assertEquals("Customer changed their mind", sale.getCancellationReason());
        assertSame(admin, sale.getCancelledBy());
        assertNotNull(sale.getCancelledAt());

        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());

        assertEquals(
                new BigDecimal("10.000"),
                lockedProduct.getCurrentStock()
        );

        assertEquals(100L, response.getSaleId());
        assertEquals(
                "REC-20260704-CANCEL02",
                response.getReceiptNumber()
        );
        assertEquals(SaleStatus.CANCELLED, response.getSaleStatus());
        assertEquals(
                new BigDecimal("24000.00"),
                response.getTotalAmount()
        );
        assertEquals(1, response.getItems().size());
        assertEquals(
                PaymentStatus.REFUNDED,
                response.getPayment().getPaymentStatus()
        );

        verify(productRepository).findByIdForUpdate(5L);
        verify(productRepository).save(lockedProduct);
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
        verify(paymentRepository).save(payment);
        verify(saleRepository).save(sale);
        verify(auditLogService).record(
                "SALE_CANCELLED",
                "SALE",
                100L,
                "Sale cancelled: REC-20260704-CANCEL02"
        );
    }
    // =========================================================
    // HELPER METHODS
    // =========================================================


    private void authenticateAsAdmin(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                )
        );
    }

    private Sale createSaleForHistory(
            Long saleId,
            String receiptNumber,
            String totalAmount,
            User cashier,
            Customer customer
    ) {
        Sale sale = createSale(receiptNumber, totalAmount);

        ReflectionTestUtils.setField(
                sale,
                "id",
                saleId
        );

        sale.setCashier(cashier);
        sale.setCustomer(customer);

        return sale;
    }

    private Payment createPaymentForHistory(
            Sale sale,
            PaymentMethod paymentMethod
    ) {
        Payment payment = new Payment();

        payment.setSale(sale);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(PaymentStatus.PAID);

        payment.setAmount(sale.getTotalAmount());
        payment.setAmountReceived(sale.getTotalAmount());
        payment.setChangeAmount(BigDecimal.ZERO);

        return payment;
    }
    private Sale createSale(
            String receiptNumber,
            String totalAmount
    ) {
        Sale sale = new Sale();

        sale.setReceiptNumber(receiptNumber);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setSubtotal(new BigDecimal(totalAmount));
        sale.setDiscountAmount(BigDecimal.ZERO);
        sale.setTotalAmount(new BigDecimal(totalAmount));

        return sale;
    }
    private Product createProductWithPrice(
            Long id,
            String name,
            String sku,
            String status,
            String currentStock,
            String sellingPrice
    ) {
        Product product = createProduct(
                id,
                name,
                sku,
                status,
                currentStock
        );

        product.setSellingPrice(new BigDecimal(sellingPrice));

        return product;
    }
    private CompleteSaleItemRequest createSaleItem(
            Long productId,
            String quantity
    ) {
        CompleteSaleItemRequest item = new CompleteSaleItemRequest();

        item.setProductId(productId);
        item.setQuantity(new BigDecimal(quantity));

        return item;
    }
    private void authenticateAsCashier(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_CASHIER")
                        )
                )
        );
    }

    private User createUser(
            Long id,
            String username,
            String status
    ) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);

        return user;
    }

    private Product createProduct(
            Long id,
            String name,
            String sku,
            String status,
            String currentStock
    ) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku(sku);
        product.setStatus(status);
        product.setCurrentStock(new BigDecimal(currentStock));

        return product;
    }

    private Customer createCustomer(
            Long id,
            String fullName,
            String phone,
            String status
    ) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setStatus(status);

        return customer;
    }
}
