package com.possystem.controller;

import com.possystem.dto.CustomerRequest;
import com.possystem.dto.CustomerResponse;
import com.possystem.dto.CustomerSearchResponse;
import com.possystem.dto.CustomerStatusRequest;
import com.possystem.dto.QuickCustomerRequest;
import com.possystem.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse createCustomer(
            @Valid @RequestBody CustomerRequest request
    ) {
        return customerService.createCustomer(request);
    }


    @PostMapping("/quick-create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public CustomerSearchResponse createQuickCustomer(
            @Valid @RequestBody QuickCustomerRequest request
    ) {
        return customerService.createQuickCustomer(request);
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public List<CustomerSearchResponse> searchActiveCustomers(
            @RequestParam String query
    ) {
        return customerService.searchActiveCustomers(query);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse getCustomerById(
            @PathVariable Long customerId
    ) {
        return customerService.getCustomerById(customerId);
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest request
    ) {
        return customerService.updateCustomer(customerId, request);
    }

    @PatchMapping("/{customerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse updateCustomerStatus(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerStatusRequest request
    ) {
        return customerService.updateCustomerStatus(
                customerId,
                request
        );
    }
}