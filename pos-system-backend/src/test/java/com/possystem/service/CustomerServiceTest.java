package com.possystem.service;

import com.possystem.dto.CustomerSearchResponse;
import com.possystem.dto.QuickCustomerRequest;
import com.possystem.entity.Customer;
import com.possystem.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);
    }

    @Test
    void createQuickCustomer_createsActiveMinimalCustomerAndReturnsSafeResponse() {

        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("  Ali Khan  ");
        request.setPhone("  +923001234567  ");

        when(customerRepository.existsByPhone("+923001234567"))
                .thenReturn(false);

        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> {
                    Customer savedCustomer = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedCustomer, "id", 10L);
                    savedCustomer.prePersist();
                    return savedCustomer;
                });

        CustomerSearchResponse response =
                customerService.createQuickCustomer(request);

        assertEquals(10L, response.getId());
        assertEquals("Ali Khan", response.getFullName());
        assertEquals("+923001234567", response.getPhone());

        ArgumentCaptor<Customer> customerCaptor =
                ArgumentCaptor.forClass(Customer.class);

        verify(customerRepository).save(customerCaptor.capture());

        Customer savedCustomer = customerCaptor.getValue();

        assertEquals("Ali Khan", savedCustomer.getFullName());
        assertEquals("+923001234567", savedCustomer.getPhone());
        assertNull(savedCustomer.getEmail());
        assertNull(savedCustomer.getAddress());
        assertEquals("ACTIVE", savedCustomer.getStatus());
    }

    @Test
    void createQuickCustomer_throwsConflict_whenPhoneAlreadyExists() {

        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("Ali Khan");
        request.setPhone("+923001234567");

        when(customerRepository.existsByPhone("+923001234567"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> customerService.createQuickCustomer(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Customer phone already exists", exception.getReason());
    }
}