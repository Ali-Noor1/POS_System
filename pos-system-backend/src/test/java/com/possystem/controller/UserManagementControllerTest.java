package com.possystem.controller;

import com.possystem.dto.CreateCashierRequest;
import com.possystem.dto.ResetCashierPasswordRequest;
import com.possystem.dto.UpdateCashierRequest;
import com.possystem.dto.UserResponse;
import com.possystem.dto.UserStatusRequest;
import com.possystem.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    @Mock
    private UserManagementService userManagementService;

    @Test
    void createCashier_returnsCreatedWithSafeUserResponse() {

        UserManagementController controller =
                new UserManagementController(userManagementService);

        CreateCashierRequest request = new CreateCashierRequest();
        request.setFullName("Cashier One");
        request.setUsername("cashier_one");
        request.setEmail("cashier.one@example.com");
        request.setPassword("Cashier@123");

        UserResponse serviceResponse = UserResponse.builder()
                .id(10L)
                .fullName("Cashier One")
                .username("cashier_one")
                .email("cashier.one@example.com")
                .roleName("CASHIER")
                .status("ACTIVE")
                .build();

        when(userManagementService.createCashier(request))
                .thenReturn(serviceResponse);

        ResponseEntity<UserResponse> response =
                controller.createCashier(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(userManagementService).createCashier(request);
    }

    @Test
    void listCashiers_returnsOkWithSafeUserResponses() {

        UserManagementController controller =
                new UserManagementController(userManagementService);

        List<UserResponse> serviceResponse = List.of(
                UserResponse.builder()
                        .id(10L)
                        .fullName("Cashier One")
                        .username("cashier_one")
                        .email("cashier.one@example.com")
                        .roleName("CASHIER")
                        .status("ACTIVE")
                        .build()
        );

        when(userManagementService.listCashiers())
                .thenReturn(serviceResponse);

        ResponseEntity<List<UserResponse>> response =
                controller.listCashiers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(userManagementService).listCashiers();
    }

    @Test
    void getCashierById_returnsOkWithSafeUserResponse() {

        UserManagementController controller =
                new UserManagementController(userManagementService);

        UserResponse serviceResponse = UserResponse.builder()
                .id(10L)
                .fullName("Cashier One")
                .username("cashier_one")
                .email("cashier.one@example.com")
                .roleName("CASHIER")
                .status("ACTIVE")
                .build();

        when(userManagementService.getCashierById(10L))
                .thenReturn(serviceResponse);

        ResponseEntity<UserResponse> response =
                controller.getCashierById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(userManagementService).getCashierById(10L);
    }

    @Test
    void updateCashierStatus_returnsOkWithSafeUserResponse() {

        UserManagementController controller =
                new UserManagementController(userManagementService);

        UserStatusRequest request = new UserStatusRequest();
        request.setStatus("INACTIVE");

        UserResponse serviceResponse = UserResponse.builder()
                .id(10L)
                .fullName("Cashier One")
                .username("cashier_one")
                .email("cashier.one@example.com")
                .roleName("CASHIER")
                .status("INACTIVE")
                .build();

        when(userManagementService.updateCashierStatus(10L, request))
                .thenReturn(serviceResponse);

        ResponseEntity<UserResponse> response =
                controller.updateCashierStatus(10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(userManagementService).updateCashierStatus(10L, request);
    }

    @Test
    void resetCashierPassword_returnsOkWithSafeUserResponse() {

        UserManagementController controller =
                new UserManagementController(userManagementService);

        ResetCashierPasswordRequest request =
                new ResetCashierPasswordRequest();
        request.setNewPassword("NewCashier@123");

        UserResponse serviceResponse = UserResponse.builder()
                .id(10L)
                .fullName("Cashier One")
                .username("cashier_one")
                .email("cashier.one@example.com")
                .roleName("CASHIER")
                .status("ACTIVE")
                .build();

        when(userManagementService.resetCashierPassword(10L, request))
                .thenReturn(serviceResponse);

        ResponseEntity<UserResponse> response =
                controller.resetCashierPassword(10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(userManagementService).resetCashierPassword(10L, request);
    }

    @Test
    void updateCashier_returnsOkWithSafeUserResponse() {

        UserManagementController controller =
                new UserManagementController(userManagementService);

        UpdateCashierRequest request = new UpdateCashierRequest();
        request.setFullName("Updated Cashier");
        request.setUsername("updated_cashier");
        request.setEmail("updated.cashier@example.com");

        UserResponse serviceResponse = UserResponse.builder()
                .id(10L)
                .fullName("Updated Cashier")
                .username("updated_cashier")
                .email("updated.cashier@example.com")
                .roleName("CASHIER")
                .status("ACTIVE")
                .build();

        when(userManagementService.updateCashier(10L, request))
                .thenReturn(serviceResponse);

        ResponseEntity<UserResponse> response =
                controller.updateCashier(10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(userManagementService).updateCashier(10L, request);
    }
}
