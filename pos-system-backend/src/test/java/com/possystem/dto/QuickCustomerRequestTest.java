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

class QuickCustomerRequestTest {

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
    void validationPasses_whenFullNameAndPhoneAreProvided() {
        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("Ali Khan");
        request.setPhone("+923001234567");

        Set<ConstraintViolation<QuickCustomerRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFails_whenFullNameIsBlank() {
        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("   ");
        request.setPhone("+923001234567");

        Set<ConstraintViolation<QuickCustomerRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Customer full name is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenPhoneIsBlank() {
        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("Ali Khan");
        request.setPhone("   ");

        Set<ConstraintViolation<QuickCustomerRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Customer phone is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenPhoneContainsInvalidCharacters() {
        QuickCustomerRequest request = new QuickCustomerRequest();
        request.setFullName("Ali Khan");
        request.setPhone("abc-123");

        Set<ConstraintViolation<QuickCustomerRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Phone number contains invalid characters",
                violations.iterator().next().getMessage()
        );
    }
}