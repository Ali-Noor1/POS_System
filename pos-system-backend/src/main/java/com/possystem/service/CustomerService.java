package com.possystem.service;

import com.possystem.dto.CustomerRequest;
import com.possystem.dto.CustomerResponse;
import com.possystem.dto.CustomerSearchResponse;
import com.possystem.dto.CustomerStatusRequest;
import com.possystem.dto.QuickCustomerRequest;
import com.possystem.entity.Customer;
import com.possystem.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {

        String fullName = request.getFullName().trim();
        String phone = request.getPhone().trim();
        String email = cleanOptionalText(request.getEmail());

        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }

        if (customerRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer phone already exists"
            );
        }

        if (email != null
                && customerRepository.existsByEmailIgnoreCase(email)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer email already exists"
            );
        }

        Customer customer = Customer.builder()
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .address(cleanOptionalText(request.getAddress()))
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        return mapToCustomerResponse(savedCustomer);
    }

    @Transactional
    public CustomerSearchResponse createQuickCustomer(
            QuickCustomerRequest request
    ) {
        String fullName = request.getFullName().trim();
        String phone = request.getPhone().trim();

        if (customerRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer phone already exists"
            );
        }

        Customer customer = Customer.builder()
                .fullName(fullName)
                .phone(phone)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        return mapToCustomerSearchResponse(savedCustomer);
    }
    @Transactional
    public CustomerResponse updateCustomer(
            Long customerId,
            CustomerRequest request
    ) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with ID: " + customerId
                ));

        String fullName = request.getFullName().trim();
        String phone = request.getPhone().trim();
        String email = cleanOptionalText(request.getEmail());

        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }

        if (customerRepository.existsByPhoneAndIdNot(phone, customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer phone already exists"
            );
        }

        if (email != null
                && customerRepository.existsByEmailIgnoreCaseAndIdNot(
                email,
                customerId
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer email already exists"
            );
        }

        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(cleanOptionalText(request.getAddress()));

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToCustomerResponse(updatedCustomer);
    }


    @Transactional
    public CustomerResponse updateCustomerStatus(
            Long customerId,
            CustomerStatusRequest request
    ) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with ID: " + customerId
                ));

        String status = request.getStatus()
                .trim()
                .toUpperCase(Locale.ROOT);

        customer.setStatus(status);

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToCustomerResponse(updatedCustomer);
    }


    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {

        return customerRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long customerId) {

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with ID: " + customerId
                ));

        return mapToCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerSearchResponse> searchActiveCustomers(
            String query
    ) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search query must contain at least 2 characters"
            );
        }

        String cleanedQuery = query.trim();

        return customerRepository
                .searchActiveCustomers(cleanedQuery)
                .stream()
                .map(this::mapToCustomerSearchResponse)
                .toList();
    }

    private CustomerSearchResponse mapToCustomerSearchResponse(
            Customer customer
    ) {
        return CustomerSearchResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .build();
    }



    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private String cleanOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}