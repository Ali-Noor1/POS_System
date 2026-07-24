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

class ResetCashierPasswordRequestTest {

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
    void validationPasses_whenNewPasswordIsValid() {
        ResetCashierPasswordRequest request =
                new ResetCashierPasswordRequest();
        request.setNewPassword("NewCashier@123");

        Set<ConstraintViolation<ResetCashierPasswordRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFails_whenNewPasswordIsBlank() {
        ResetCashierPasswordRequest request =
                new ResetCashierPasswordRequest();
        request.setNewPassword("   ");

        Set<ConstraintViolation<ResetCashierPasswordRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "New password is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenNewPasswordIsTooShort() {
        ResetCashierPasswordRequest request =
                new ResetCashierPasswordRequest();
        request.setNewPassword("short");

        Set<ConstraintViolation<ResetCashierPasswordRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "New password must be at least 8 characters",
                violations.iterator().next().getMessage()
        );
    }
}
