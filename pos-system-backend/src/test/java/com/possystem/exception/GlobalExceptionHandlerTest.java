package com.possystem.exception;

import com.possystem.dto.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returnsBadRequestJson() {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/reports/sales");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException("Invalid date range"),
                        request
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid date range", response.getBody().getMessage());
        assertEquals("/api/reports/sales", response.getBody().getPath());
    }

    @Test
    void handleResponseStatusException_usesExceptionStatusAndReason() {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/products/99");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleResponseStatusException(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        ),
                        request
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Product not found", response.getBody().getMessage());
        assertEquals("/api/products/99", response.getBody().getPath());
    }

    @Test
    void handleAccessDenied_returnsForbiddenJson() {

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/admin/users");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleAccessDenied(
                        new AccessDeniedException("Denied"),
                        request
                );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Access denied", response.getBody().getMessage());
        assertEquals("/api/admin/users", response.getBody().getPath());
    }

    @Test
    void handleValidation_returnsFieldErrors() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/admin/users");
        ValidationRequest target = new ValidationRequest();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "request");
        bindingResult.rejectValue(
                "name",
                "NotBlank",
                "Name is required"
        );

        Method method = SampleController.class.getDeclaredMethod(
                "create",
                ValidationRequest.class
        );
        MethodParameter parameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        parameter,
                        bindingResult
                );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals("/api/admin/users", response.getBody().getPath());
        assertEquals(
                "Name is required",
                response.getBody().getValidationErrors().get("name")
        );
    }

    @Test
    void handleGenericException_returnsInternalServerErrorJson() {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/reports/sales");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleGenericException(
                        new RuntimeException("Database details"),
                        request
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals(
                "An unexpected error occurred",
                response.getBody().getMessage()
        );
        assertEquals("/api/reports/sales", response.getBody().getPath());
    }

    private static class ValidationRequest {
        @NotBlank
        private String name;

        public String getName() {
            return name;
        }
    }

    private static class SampleController {
        @SuppressWarnings("unused")
        void create(@Valid ValidationRequest request) {
        }
    }
}
