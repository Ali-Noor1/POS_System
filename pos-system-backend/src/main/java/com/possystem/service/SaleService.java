package com.possystem.service;

import com.possystem.dto.*;
import com.possystem.entity.*;
import com.possystem.repository.*;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SaleService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptNumberService receiptNumberService;
    private final AuditLogService auditLogService;

    public SaleService(
            UserRepository userRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            PaymentRepository paymentRepository,
            ReceiptNumberService receiptNumberService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.inventoryTransactionRepository =
                inventoryTransactionRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.paymentRepository = paymentRepository;
        this.receiptNumberService = receiptNumberService;
        this.auditLogService = auditLogService;
    }
    // =========================================================
    // AUTHENTICATED CASHIER / ADMIN
    // =========================================================

    /*
     * Gets the user from the current JWT / Spring Security context.
     *
     * Frontend must never send cashierId.
     * The backend identifies the cashier from the authenticated JWT user.
     */
    public User getAuthenticatedSaleUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new AccessDeniedException(
                    "Authenticated user is required to complete a sale"
            );
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user was not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AccessDeniedException(
                    "Inactive user cannot complete a sale"
            );
        }

        return user;
    }

    // =========================================================
    // PRODUCT LOADING AND VALIDATION
    // =========================================================

    /*
     * Loads a product using PESSIMISTIC_WRITE locking.
     *
     * Later, during complete checkout, this prevents two cashiers
     * from changing or selling the same product stock incorrectly.
     */
    @Transactional
    public Product loadActiveProductForSale(Long productId) {

        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product ID must be greater than zero"
            );
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive product cannot be sold"
            );
        }

        return product;
    }

    /*
     * Validates the requested quantity against the product stock.
     *
     * This method does not reduce stock yet.
     * Actual stock reduction will happen later inside completeSale().
     */
    public void validateQuantityAndStockForSale(
            Product product,
            BigDecimal requestedQuantity
    ) {
        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product is required"
            );
        }

        if (requestedQuantity == null
                || requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale quantity must be greater than zero"
            );
        }

        BigDecimal currentStock = product.getCurrentStock();

        if (currentStock == null) {
            currentStock = BigDecimal.ZERO;
        }

        if (currentStock.compareTo(requestedQuantity) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock for product: " + product.getName()
            );
        }
    }

    // =========================================================
    // CUSTOMER LOADING AND VALIDATION
    // =========================================================

    /*
     * customerId is optional.
     *
     * null means a walk-in sale.
     * A supplied customer must exist and must be ACTIVE.
     */
    public Customer loadActiveCustomerForSale(Long customerId) {

        // Walk-in customer sale
        if (customerId == null) {
            return null;
        }

        if (customerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID must be greater than zero"
            );
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));

        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive customer cannot be selected for a sale"
            );
        }

        return customer;
    }

    // =========================================================
// PAYMENT VALIDATION AND CHANGE CALCULATION
// =========================================================

    /*
     * Validates payment input and calculates the change amount.
     *
     * CASH:
     * - amountReceived can be equal to or greater than totalAmount.
     * - changeAmount = amountReceived - totalAmount.
     *
     * CARD / BANK_TRANSFER / MOBILE_WALLET:
     * - amountReceived must exactly match totalAmount.
     * - changeAmount is always zero.
     */
    public BigDecimal calculateChangeAmount(
            PaymentMethod paymentMethod,
            BigDecimal totalAmount,
            BigDecimal amountReceived
    ) {
        if (paymentMethod == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment method is required"
            );
        }

        if (totalAmount == null
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale total must be greater than zero"
            );
        }

        if (amountReceived == null
                || amountReceived.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount received must be greater than zero"
            );
        }

        if (paymentMethod == PaymentMethod.CASH) {

            if (amountReceived.compareTo(totalAmount) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cash received is less than the sale total"
                );
            }

            return amountReceived.subtract(totalAmount);
        }

        if (amountReceived.compareTo(totalAmount) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment amount must exactly match the sale total"
            );
        }

        return BigDecimal.ZERO.setScale(2);
    }

    // =========================================================
// CART ITEM MERGING
// =========================================================

    /*
     * Combines quantities when the same product appears more than once
     * in the checkout request.
     *
     * Example:
     * Product 5, quantity 1.000
     * Product 5, quantity 2.000
     *
     * Result:
     * Product 5, quantity 3.000
     *
     * LinkedHashMap preserves the original product scan/order sequence.
     */
    public Map<Long, BigDecimal> mergeSaleItemQuantities(
            List<CompleteSaleItemRequest> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one sale item is required"
            );
        }

        Map<Long, BigDecimal> quantitiesByProductId =
                new LinkedHashMap<>();

        for (CompleteSaleItemRequest item : items) {

            if (item == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sale item is required"
                );
            }

            Long productId = item.getProductId();

            if (productId == null || productId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product ID must be greater than zero"
                );
            }

            BigDecimal quantity = item.getQuantity();

            if (quantity == null
                    || quantity.compareTo(BigDecimal.ZERO) <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sale quantity must be greater than zero"
                );
            }

            quantitiesByProductId.merge(
                    productId,
                    quantity,
                    BigDecimal::add
            );
        }

        return quantitiesByProductId;
    }

    // =========================================================
// SALE PRICE AND LINE TOTAL CALCULATION
// =========================================================

    /*
     * Calculates the total for one unique sale item.
     *
     * Price always comes from Product.sellingPrice in the database.
     * The frontend must never send or control the final selling price.
     *
     * Current Version 1 formula:
     * lineTotal = sellingPrice × quantity
     *
     * Item-level discounts will be added later if needed.
     */
    public BigDecimal calculateSaleLineTotal(
            Product product,
            BigDecimal requestedQuantity
    ) {
        /*
         * Reuses existing protection:
         * - product must exist
         * - quantity must be greater than zero
         * - stock must be sufficient
         */
        validateQuantityAndStockForSale(
                product,
                requestedQuantity
        );

        BigDecimal sellingPrice = product.getSellingPrice();

        if (sellingPrice == null
                || sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product selling price must be greater than zero"
            );
        }

        return sellingPrice
                .multiply(requestedQuantity)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
// SALE STOCK REDUCTION AND INVENTORY HISTORY
// =========================================================

    /*
     * Reduces stock for a completed sale and creates one SALE
     * inventory transaction record.
     *
     * Important:
     * - product must already be loaded using findByIdForUpdate(...)
     * - saleId must exist because it is stored as referenceId
     * - this will later run inside the final completeSale() transaction
     */
    @Transactional
    public InventoryTransaction reduceStockAndCreateSaleInventoryTransaction(
            Product product,
            BigDecimal soldQuantity,
            Long saleId,
            User cashier
    ) {
        if (saleId == null || saleId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale ID must be greater than zero"
            );
        }

        if (cashier == null || cashier.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authenticated cashier is required"
            );
        }

        /*
         * Reuses the existing rule:
         * - product must not be null
         * - quantity must be greater than zero
         * - stock must be sufficient
         */
        validateQuantityAndStockForSale(product, soldQuantity);

        BigDecimal stockBefore = product.getCurrentStock();

        BigDecimal stockAfter = stockBefore.subtract(soldQuantity);

        /*
         * This changes the actual product stock.
         */
        product.setCurrentStock(stockAfter);

        InventoryTransaction inventoryTransaction =
                new InventoryTransaction();

        inventoryTransaction.setProduct(product);
        inventoryTransaction.setTransactionType(
                InventoryTransactionType.SALE
        );

        /*
         * Sale removes stock, so quantity change is negative.
         *
         * Example:
         * soldQuantity = 3.000
         * quantityChange = -3.000
         */
        inventoryTransaction.setQuantityChange(
                soldQuantity.negate()
        );

        inventoryTransaction.setStockBefore(stockBefore);
        inventoryTransaction.setStockAfter(stockAfter);

        inventoryTransaction.setReferenceType("SALE");
        inventoryTransaction.setReferenceId(saleId);

        inventoryTransaction.setNote(
                "Stock reduced after completed sale"
        );

        inventoryTransaction.setCreatedBy(cashier);

        productRepository.save(product);

        return inventoryTransactionRepository.save(
                inventoryTransaction
        );
    }

    // =========================================================
// SALE CREATION
// =========================================================

    /*
     * Creates and saves the main Sale record.
     *
     * At this stage:
     * - customer may be null for a walk-in sale
     * - cashier must always be present
     * - receipt number is generated only by backend
     * - sale status always starts as COMPLETED
     *
     * Sale items, payment, stock reduction, and inventory history
     * will be attached later inside the final completeSale() transaction.
     */
    @Transactional
    public Sale createCompletedSale(
            Customer customer,
            User cashier,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
        if (cashier == null || cashier.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authenticated cashier is required"
            );
        }

        if (subtotal == null
                || subtotal.compareTo(BigDecimal.ZERO) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale subtotal cannot be negative"
            );
        }

        if (discountAmount == null
                || discountAmount.compareTo(BigDecimal.ZERO) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount amount cannot be negative"
            );
        }

        if (totalAmount == null
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale total must be greater than zero"
            );
        }

        BigDecimal expectedTotal = subtotal.subtract(discountAmount);

        if (expectedTotal.compareTo(totalAmount) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale total does not match subtotal and discount"
            );
        }

        Sale sale = new Sale();

        sale.setReceiptNumber(
                receiptNumberService.generateUniqueReceiptNumber()
        );

        sale.setCustomer(customer);
        sale.setCashier(cashier);

        sale.setStatus(SaleStatus.COMPLETED);

        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmount);
        sale.setTotalAmount(totalAmount);

        return saleRepository.save(sale);
    }

    // =========================================================
// SALE ITEM CREATION
// =========================================================

    /*
     * Creates one SaleItem snapshot for one unique product.
     *
     * Product name, SKU, selling price, and line total are taken
     * from the backend product record, never from frontend input.
     *
     * Current Version 1:
     * - item discount is always 0.00
     * - line total = unit price × quantity
     */
    @Transactional
    public SaleItem createSaleItem(
            Sale sale,
            Product product,
            BigDecimal quantity
    ) {
        if (sale == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale is required"
            );
        }

        /*
         * This validates:
         * - product is not null
         * - quantity is greater than zero
         * - product has enough stock
         * - selling price is greater than zero
         */
        BigDecimal lineTotal = calculateSaleLineTotal(
                product,
                quantity
        );

        SaleItem saleItem = new SaleItem();

        saleItem.setSale(sale);
        saleItem.setProduct(product);

        /*
         * Snapshot values:
         * They protect old receipts if the product is edited later.
         */
        saleItem.setProductName(product.getName());
        saleItem.setProductSku(product.getSku());

        saleItem.setQuantity(quantity);

        saleItem.setUnitPrice(
                product.getSellingPrice()
                        .setScale(2, RoundingMode.HALF_UP)
        );

        saleItem.setDiscountAmount(
                BigDecimal.ZERO.setScale(2)
        );

        saleItem.setLineTotal(lineTotal);

        return saleItemRepository.save(saleItem);
    }


    // =========================================================
// PAYMENT CREATION
// =========================================================

    /*
     * Creates one PAID payment record for a completed sale.
     *
     * Payment.amount:
     * - always equals the sale total
     *
     * Payment.amountReceived:
     * - cash received from the customer
     * - for non-cash payment, must equal sale total
     *
     * Payment.changeAmount:
     * - cash received minus sale total
     * - always 0.00 for non-cash methods
     */
    @Transactional
    public Payment createPaidPayment(
            Sale sale,
            PaymentMethod paymentMethod,
            BigDecimal amountReceived,
            String referenceNumber
    ) {
        if (sale == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale is required"
            );
        }

        BigDecimal totalAmount = sale.getTotalAmount();

        /*
         * This validates:
         * - payment method exists
         * - sale total is greater than zero
         * - amount received is valid
         * - cash has enough received amount
         * - non-cash payment exactly matches total
         *
         * It also calculates cash change.
         */
        BigDecimal changeAmount = calculateChangeAmount(
                paymentMethod,
                totalAmount,
                amountReceived
        );

        Payment payment = new Payment();

        payment.setSale(sale);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(PaymentStatus.PAID);

        /*
         * Amount applied to the sale.
         * It is always equal to the saved sale total.
         */
        payment.setAmount(
                totalAmount.setScale(2, RoundingMode.HALF_UP)
        );

        payment.setAmountReceived(
                amountReceived.setScale(2, RoundingMode.HALF_UP)
        );

        payment.setChangeAmount(
                changeAmount.setScale(2, RoundingMode.HALF_UP)
        );

        payment.setReferenceNumber(
                cleanOptionalText(referenceNumber)
        );

        return paymentRepository.save(payment);
    }


    // =========================================================
    // SALE CANCELLATION VALIDATION
    // =========================================================

    /*
     * Loads a sale that is eligible for cancellation.
     *
     * Version 1 cancellation rule:
     * - only COMPLETED sales can be cancelled
     * - already CANCELLED sales are blocked
     */
    public Sale loadCompletedSaleForCancellation(Long saleId) {

        if (saleId == null || saleId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale ID must be greater than zero"
            );
        }

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found"
                ));

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only completed sales can be cancelled"
            );
        }

        return sale;
    }

    @Transactional
    public InventoryTransaction restoreStockAndCreateSaleCancellationInventoryTransaction(
            SaleItem saleItem,
            Long saleId,
            User cancelledBy
    ) {
        if (saleItem == null || saleItem.getProduct() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale item product is required"
            );
        }

        if (saleId == null || saleId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale ID must be greater than zero"
            );
        }

        if (cancelledBy == null || cancelledBy.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authenticated admin is required"
            );
        }

        BigDecimal restoredQuantity = saleItem.getQuantity();

        if (restoredQuantity == null
                || restoredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale item quantity must be greater than zero"
            );
        }

        Product product = productRepository
                .findByIdForUpdate(saleItem.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        BigDecimal stockBefore = product.getCurrentStock();

        if (stockBefore == null) {
            stockBefore = BigDecimal.ZERO;
        }

        BigDecimal stockAfter = stockBefore.add(restoredQuantity);

        product.setCurrentStock(stockAfter);

        InventoryTransaction inventoryTransaction =
                new InventoryTransaction();

        inventoryTransaction.setProduct(product);
        inventoryTransaction.setTransactionType(
                InventoryTransactionType.SALE_CANCELLED
        );
        inventoryTransaction.setQuantityChange(restoredQuantity);
        inventoryTransaction.setStockBefore(stockBefore);
        inventoryTransaction.setStockAfter(stockAfter);
        inventoryTransaction.setReferenceType("SALE");
        inventoryTransaction.setReferenceId(saleId);
        inventoryTransaction.setNote(
                "Stock restored after sale cancellation"
        );
        inventoryTransaction.setCreatedBy(cancelledBy);

        productRepository.save(product);

        return inventoryTransactionRepository.save(
                inventoryTransaction
        );
    }
    public String validateCancellationReason(String cancellationReason) {

        if (cancellationReason == null || cancellationReason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancellation reason is required"
            );
        }

        String cleanedReason = cancellationReason.trim();

        if (cleanedReason.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancellation reason must not exceed 500 characters"
            );
        }

        return cleanedReason;
    }
    @Transactional
    public CompleteSaleResponse cancelSale(
            Long saleId,
            CancelSaleRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancellation request is required"
            );
        }

        User cancelledBy = getAuthenticatedSaleUser();

        if (!currentUserHasAdminRole()) {
            throw new AccessDeniedException(
                    "Only admin can cancel sales"
            );
        }

        String cancellationReason = validateCancellationReason(
                request.getCancellationReason()
        );

        Sale sale = loadCompletedSaleForCancellation(saleId);

        List<SaleItem> saleItems =
                saleItemRepository.findBySaleIdOrderByIdAsc(saleId);

        if (saleItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Sale items not found for sale"
            );
        }

        List<Payment> payments = paymentRepository
                .findBySaleIdOrderByCreatedAtAsc(saleId);

        if (payments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment record not found for sale"
            );
        }

        for (SaleItem saleItem : saleItems) {
            restoreStockAndCreateSaleCancellationInventoryTransaction(
                    saleItem,
                    sale.getId(),
                    cancelledBy
            );
        }

        for (Payment payment : payments) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancellationReason(cancellationReason);
        sale.setCancelledAt(java.time.LocalDateTime.now());
        sale.setCancelledBy(cancelledBy);

        Sale savedSale = saleRepository.save(sale);

        auditLogService.record(
                "SALE_CANCELLED",
                "SALE",
                savedSale.getId(),
                "Sale cancelled: " + savedSale.getReceiptNumber()
        );

        return mapToCompleteSaleResponse(
                savedSale,
                saleItems,
                payments.getFirst()
        );
    }
    // =========================================================
    // SALE DETAIL / RECEIPT VIEW
    // =========================================================

    /*
     * Returns one complete receipt/sale detail.
     *
     * ADMIN:
     * - can view any sale
     *
     * CASHIER:
     * - can view only a sale completed by that cashier
     *
     * No cost price is returned because this response uses
     * safe receipt DTOs only.
     */
    @Transactional(readOnly = true)
    public CompleteSaleResponse getSaleDetails(Long saleId) {

        if (saleId == null || saleId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale ID must be greater than zero"
            );
        }

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found"
                ));

        User currentUser = getAuthenticatedSaleUser();

        validateSaleDetailAccess(
                sale,
                currentUser
        );

        List<SaleItem> saleItems =
                saleItemRepository.findBySaleIdOrderByIdAsc(
                        saleId
                );

        /*
         * Version 1 has one payment per sale.
         *
         * The database remains ready for split payment later,
         * but CompleteSaleResponse currently returns one payment.
         */
        Payment payment = paymentRepository
                .findBySaleIdOrderByCreatedAtAsc(saleId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment record not found for sale"
                ));

        return mapToSaleDetailResponse(
                sale,
                saleItems,
                payment
        );
    }

    /*
     * ADMIN may view any sale.
     * A CASHIER may view only their own sale.
     */
    private void validateSaleDetailAccess(
            Sale sale,
            User currentUser
    ) {
        if (currentUserHasAdminRole()) {
            return;
        }

        if (sale.getCashier() == null
                || sale.getCashier().getId() == null
                || currentUser.getId() == null
                || !sale.getCashier().getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Cashier can only view their own sales"
            );
        }
    }

    private boolean currentUserHasAdminRole() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                );
    }

    // =========================================================
    // SALE DETAIL RESPONSE MAPPING
    // =========================================================

    private CompleteSaleResponse mapToSaleDetailResponse(
            Sale sale,
            List<SaleItem> saleItems,
            Payment payment
    ) {
        CompleteSaleResponse response =
                new CompleteSaleResponse();

        response.setSaleId(sale.getId());
        response.setReceiptNumber(sale.getReceiptNumber());

        if (sale.getCustomer() != null) {
            response.setCustomerId(
                    sale.getCustomer().getId()
            );

            response.setCustomerName(
                    sale.getCustomer().getFullName()
            );
        }

        response.setCashierId(
                sale.getCashier().getId()
        );

        response.setCashierUsername(
                sale.getCashier().getUsername()
        );

        response.setSaleStatus(sale.getStatus());
        response.setSubtotal(sale.getSubtotal());

        response.setDiscountAmount(
                sale.getDiscountAmount()
        );

        response.setTotalAmount(
                sale.getTotalAmount()
        );

        response.setAmountReceived(
                payment.getAmountReceived()
        );

        response.setChangeAmount(
                payment.getChangeAmount()
        );

        response.setPayment(
                mapToSaleDetailPaymentResponse(payment)
        );

        response.setItems(
                saleItems.stream()
                        .map(this::mapToSaleDetailItemResponse)
                        .toList()
        );

        response.setCompletedAt(
                sale.getCreatedAt()
        );

        return response;
    }

    private SaleItemResponse mapToSaleDetailItemResponse(
            SaleItem saleItem
    ) {
        SaleItemResponse response =
                new SaleItemResponse();

        response.setId(saleItem.getId());

        response.setProductId(
                saleItem.getProduct().getId()
        );

        response.setProductName(
                saleItem.getProductName()
        );

        response.setProductSku(
                saleItem.getProductSku()
        );

        response.setQuantity(
                saleItem.getQuantity()
        );

        response.setUnitPrice(
                saleItem.getUnitPrice()
        );

        response.setDiscountAmount(
                saleItem.getDiscountAmount()
        );

        response.setLineTotal(
                saleItem.getLineTotal()
        );

        return response;
    }

    private PaymentResponse mapToSaleDetailPaymentResponse(
            Payment payment
    ) {
        PaymentResponse response =
                new PaymentResponse();

        response.setId(payment.getId());

        response.setPaymentMethod(
                payment.getPaymentMethod()
        );

        response.setPaymentStatus(
                payment.getPaymentStatus()
        );

        response.setAmount(payment.getAmount());

        response.setReferenceNumber(
                payment.getReferenceNumber()
        );

        response.setPaidAt(payment.getPaidAt());

        return response;
    }

    // =========================================================
    // SALES HISTORY
    // =========================================================

    /*
     * ADMIN:
     * - sees all sales, newest first
     *
     * CASHIER:
     * - sees only their own sales, newest first
     */
    @Transactional(readOnly = true)
    public List<SaleHistoryResponse> getSalesHistory() {

        User currentUser = getAuthenticatedSaleUser();

        List<Sale> sales;

        if (currentUserHasAdminRole()) {
            sales = saleRepository.findAllByOrderByCreatedAtDesc();
        } else {
            sales = saleRepository
                    .findByCashier_IdOrderByCreatedAtDesc(
                            currentUser.getId()
                    );
        }

        return sales.stream()
                .map(this::mapToSaleHistoryResponse)
                .toList();
    }

    private SaleHistoryResponse mapToSaleHistoryResponse(
            Sale sale
    ) {
        SaleHistoryResponse response =
                new SaleHistoryResponse();

        response.setSaleId(sale.getId());
        response.setReceiptNumber(sale.getReceiptNumber());

        if (sale.getCustomer() != null) {
            response.setCustomerId(
                    sale.getCustomer().getId()
            );

            response.setCustomerName(
                    sale.getCustomer().getFullName()
            );
        }

        response.setCashierId(
                sale.getCashier().getId()
        );

        response.setCashierUsername(
                sale.getCashier().getUsername()
        );

        response.setSaleStatus(sale.getStatus());
        response.setTotalAmount(sale.getTotalAmount());

        /*
         * Version 1 creates one payment per sale.
         * Later, when split payments are added, this can become
         * a list of payment methods.
         */
        PaymentMethod paymentMethod = paymentRepository
                .findBySaleIdOrderByCreatedAtAsc(sale.getId())
                .stream()
                .findFirst()
                .map(Payment::getPaymentMethod)
                .orElse(null);

        response.setPaymentMethod(paymentMethod);
        response.setCreatedAt(sale.getCreatedAt());

        return response;
    }

    private String cleanOptionalText(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // =========================================================
    // COMPLETE SALE TRANSACTION
    // =========================================================

    /*
     * Main POS checkout transaction.
     *
     * Everything happens in one database transaction:
     *
     * 1. Read logged-in cashier from JWT
     * 2. Validate selected customer or allow walk-in
     * 3. Merge duplicate product scans
     * 4. Lock each product row
     * 5. Validate product, stock, quantity, and price
     * 6. Calculate totals from database prices
     * 7. Validate payment
     * 8. Create Sale record
     * 9. Create SaleItem records
     * 10. Create Payment record
     * 11. Reduce stock
     * 12. Create SALE inventory history
     * 13. Return safe receipt response
     *
     * If any one step fails, Spring rolls back all changes.
     */
    @Transactional
    public CompleteSaleResponse completeSale(
            CompleteSaleRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sale request is required"
            );
        }

        if (request.getPayment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment information is required"
            );
        }

        /*
         * Cashier comes from JWT/Spring Security.
         * Frontend never provides cashierId.
         */
        User cashier = getAuthenticatedSaleUser();

        /*
         * customerId may be null for walk-in sales.
         */
        Customer customer = loadActiveCustomerForSale(
                request.getCustomerId()
        );

        /*
         * Combine duplicate scans before product locking.
         *
         * Example:
         * Product 5, quantity 1
         * Product 5, quantity 2
         *
         * Result:
         * Product 5, quantity 3
         */
        Map<Long, BigDecimal> quantitiesByProductId =
                mergeSaleItemQuantities(request.getItems());

        List<PreparedSaleItem> preparedSaleItems =
                new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

        /*
         * Load every product using PESSIMISTIC_WRITE locking.
         * Validate stock and calculate total from actual DB price.
         */
        for (Map.Entry<Long, BigDecimal> entry
                : quantitiesByProductId.entrySet()) {

            Long productId = entry.getKey();
            BigDecimal quantity = entry.getValue();

            Product product = loadActiveProductForSale(productId);

            BigDecimal lineTotal = calculateSaleLineTotal(
                    product,
                    quantity
            );

            preparedSaleItems.add(
                    new PreparedSaleItem(
                            product,
                            quantity
                    )
            );

            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        /*
         * Version 1:
         * No discount functionality yet.
         */
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);

        BigDecimal totalAmount = subtotal
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        /*
         * Validate payment before persisting anything.
         *
         * createPaidPayment() will call this again while creating
         * the Payment entity, but validating here keeps the checkout
         * flow easy to understand and fails early.
         */
        calculateChangeAmount(
                request.getPayment().getPaymentMethod(),
                totalAmount,
                request.getPayment().getAmountReceived()
        );

        /*
         * Save the Sale first, so MySQL/Hibernate generates sale ID.
         * That ID is required in inventory transaction referenceId.
         */
        Sale sale = createCompletedSale(
                customer,
                cashier,
                subtotal,
                discountAmount,
                totalAmount
        );

        if (sale.getId() == null) {
            throw new IllegalStateException(
                    "Sale ID was not generated"
            );
        }

        List<SaleItem> savedSaleItems = new ArrayList<>();

        /*
         * Create sale-item snapshots and inventory SALE history.
         */
        for (PreparedSaleItem preparedSaleItem
                : preparedSaleItems) {

            SaleItem saleItem = createSaleItem(
                    sale,
                    preparedSaleItem.product(),
                    preparedSaleItem.quantity()
            );

            savedSaleItems.add(saleItem);

            reduceStockAndCreateSaleInventoryTransaction(
                    preparedSaleItem.product(),
                    preparedSaleItem.quantity(),
                    sale.getId(),
                    cashier
            );
        }

        /*
         * Payment amount is always the sale total.
         * amountReceived and changeAmount are saved permanently.
         */
        Payment payment = createPaidPayment(
                sale,
                request.getPayment().getPaymentMethod(),
                request.getPayment().getAmountReceived(),
                request.getPayment().getReferenceNumber()
        );

        return mapToCompleteSaleResponse(
                sale,
                savedSaleItems,
                payment
        );
    }

    // =========================================================
    // RECEIPT RESPONSE MAPPING
    // =========================================================

    private CompleteSaleResponse mapToCompleteSaleResponse(
            Sale sale,
            List<SaleItem> saleItems,
            Payment payment
    ) {
        CompleteSaleResponse response =
                new CompleteSaleResponse();

        response.setSaleId(sale.getId());
        response.setReceiptNumber(sale.getReceiptNumber());

        if (sale.getCustomer() != null) {
            response.setCustomerId(
                    sale.getCustomer().getId()
            );

            response.setCustomerName(
                    sale.getCustomer().getFullName()
            );
        }

        response.setCashierId(
                sale.getCashier().getId()
        );

        response.setCashierUsername(
                sale.getCashier().getUsername()
        );

        response.setSaleStatus(sale.getStatus());

        response.setSubtotal(sale.getSubtotal());
        response.setDiscountAmount(
                sale.getDiscountAmount()
        );

        response.setTotalAmount(
                sale.getTotalAmount()
        );

        response.setAmountReceived(
                payment.getAmountReceived()
        );

        response.setChangeAmount(
                payment.getChangeAmount()
        );

        response.setPayment(
                mapToPaymentResponse(payment)
        );

        response.setItems(
                saleItems.stream()
                        .map(this::mapToSaleItemResponse)
                        .toList()
        );

        response.setCompletedAt(sale.getCreatedAt());

        return response;
    }

    private SaleItemResponse mapToSaleItemResponse(
            SaleItem saleItem
    ) {
        SaleItemResponse response =
                new SaleItemResponse();

        response.setId(saleItem.getId());

        response.setProductId(
                saleItem.getProduct().getId()
        );

        response.setProductName(
                saleItem.getProductName()
        );

        response.setProductSku(
                saleItem.getProductSku()
        );

        response.setQuantity(
                saleItem.getQuantity()
        );

        response.setUnitPrice(
                saleItem.getUnitPrice()
        );

        response.setDiscountAmount(
                saleItem.getDiscountAmount()
        );

        response.setLineTotal(
                saleItem.getLineTotal()
        );

        return response;
    }

    private PaymentResponse mapToPaymentResponse(
            Payment payment
    ) {
        PaymentResponse response =
                new PaymentResponse();

        response.setId(payment.getId());

        response.setPaymentMethod(
                payment.getPaymentMethod()
        );

        response.setPaymentStatus(
                payment.getPaymentStatus()
        );

        response.setAmount(payment.getAmount());

        response.setReferenceNumber(
                payment.getReferenceNumber()
        );

        response.setPaidAt(payment.getPaidAt());

        return response;
    }

    /*
     * Internal prepared cart line.
     *
     * Product is the locked database product.
     * Quantity is already merged and validated.
     */
    private record PreparedSaleItem(
            Product product,
            BigDecimal quantity
    ) {
    }
}
