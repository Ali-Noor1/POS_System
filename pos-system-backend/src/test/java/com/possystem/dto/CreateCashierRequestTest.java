package com.possystem.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCashierRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validationPasses_whenAllFieldsAreValid() {
        CreateCashierRequest request = validRequest();

        Set<ConstraintViolation<CreateCashierRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFails_whenFullNameIsBlank() {
        CreateCashierRequest request = validRequest();
        request.setFullName("   ");

        Set<ConstraintViolation<CreateCashierRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Full name is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenEmailIsInvalid() {
        CreateCashierRequest request = validRequest();
        request.setEmail("not-an-email");

        Set<ConstraintViolation<CreateCashierRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Email format is invalid",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenPasswordIsTooShort() {
        CreateCashierRequest request = validRequest();
        request.setPassword("short");

        Set<ConstraintViolation<CreateCashierRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Password must be at least 8 characters",
                violations.iterator().next().getMessage()
        );
    }

    private CreateCashierRequest validRequest() {
        CreateCashierRequest request = new CreateCashierRequest();
        request.setFullName("Cashier One");
        request.setUsername("cashier_one");
        request.setEmail("cashier.one@example.com");
        request.setPassword("Cashier@123");
        return request;
    }
}