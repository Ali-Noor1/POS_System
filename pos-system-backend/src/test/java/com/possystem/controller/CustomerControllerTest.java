package com.possystem.controller;

import com.possystem.dto.CustomerSearchResponse;
import com.possystem.dto.QuickCustomerRequest;
import com.possystem.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @Test
    void createQuickCustomer_delegatesToServiceAndReturnsSafeResponse() {

        CustomerController controller = new CustomerController(customerService);

        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("Ali Khan");
        request.setPhone("+923001234567");

        CustomerSearchResponse serviceResponse = CustomerSearchResponse.builder()
                .id(10L)
                .fullName("Ali Khan")
                .phone("+923001234567")
                .build();

        when(customerService.createQuickCustomer(request))
                .thenReturn(serviceResponse);

        CustomerSearchResponse response = controller.createQuickCustomer(request);

        assertSame(serviceResponse, response);
        verify(customerService).createQuickCustomer(request);
    }
}