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

class UpdateCashierRequestTest {

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
        UpdateCashierRequest request = validRequest();

        Set<ConstraintViolation<UpdateCashierRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFails_whenFullNameIsBlank() {
        UpdateCashierRequest request = validRequest();
        request.setFullName("   ");

        Set<ConstraintViolation<UpdateCashierRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Full name is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenEmailIsInvalid() {
        UpdateCashierRequest request = validRequest();
        request.setEmail("not-an-email");

        Set<ConstraintViolation<UpdateCashierRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Email format is invalid",
                violations.iterator().next().getMessage()
        );
    }

    private UpdateCashierRequest validRequest() {
        UpdateCashierRequest request = new UpdateCashierRequest();
        request.setFullName("Updated Cashier");
        request.setUsername("updated_cashier");
        request.setEmail("updated.cashier@example.com");
        return request;
    }
}
