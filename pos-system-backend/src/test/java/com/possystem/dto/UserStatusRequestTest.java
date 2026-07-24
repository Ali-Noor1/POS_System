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

class UserStatusRequestTest {

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
    void validationPasses_whenStatusIsActiveOrInactive() {
        UserStatusRequest request = new UserStatusRequest();
        request.setStatus("active");

        Set<ConstraintViolation<UserStatusRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFails_whenStatusIsBlank() {
        UserStatusRequest request = new UserStatusRequest();
        request.setStatus("   ");

        Set<ConstraintViolation<UserStatusRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "User status is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenStatusIsUnsupported() {
        UserStatusRequest request = new UserStatusRequest();
        request.setStatus("LOCKED");

        Set<ConstraintViolation<UserStatusRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "User status must be ACTIVE or INACTIVE",
                violations.iterator().next().getMessage()
        );
    }
}
