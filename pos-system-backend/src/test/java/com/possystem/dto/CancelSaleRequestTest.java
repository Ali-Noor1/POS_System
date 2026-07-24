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

class CancelSaleRequestTest {

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
    void validationPasses_whenCancellationReasonIsProvided() {
        CancelSaleRequest request = new CancelSaleRequest();
        request.setCancellationReason("Customer returned the items before leaving the shop");

        Set<ConstraintViolation<CancelSaleRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFails_whenCancellationReasonIsBlank() {
        CancelSaleRequest request = new CancelSaleRequest();
        request.setCancellationReason("   ");

        Set<ConstraintViolation<CancelSaleRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Cancellation reason is required",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validationFails_whenCancellationReasonExceedsFiveHundredCharacters() {
        CancelSaleRequest request = new CancelSaleRequest();
        request.setCancellationReason("a".repeat(501));

        Set<ConstraintViolation<CancelSaleRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals(
                "Cancellation reason must not exceed 500 characters",
                violations.iterator().next().getMessage()
        );
    }
}