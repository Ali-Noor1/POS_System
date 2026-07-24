package com.possystem.service;

import com.possystem.repository.SaleRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceiptNumberServiceTest {

    @Test
    void generateUniqueReceiptNumber_returnsCorrectFormat_whenReceiptIsAvailable() {

        SaleRepository saleRepository = mock(SaleRepository.class);
        when(saleRepository.existsByReceiptNumber(anyString())).thenReturn(false);

        ReceiptNumberService receiptNumberService =
                new ReceiptNumberService(saleRepository);

        String receiptNumber = receiptNumberService.generateUniqueReceiptNumber();

        assertTrue(
                receiptNumber.matches("REC-\\d{8}-[A-F0-9]{8}"),
                "Receipt number must match format REC-yyyyMMdd-XXXXXXXX"
        );

        verify(saleRepository).existsByReceiptNumber(receiptNumber);
    }

    @Test
    void generateUniqueReceiptNumber_retries_whenGeneratedNumberAlreadyExists() {

        SaleRepository saleRepository = mock(SaleRepository.class);

        /*
         * First generated receipt is treated as already existing.
         * Second generated receipt is available.
         */
        when(saleRepository.existsByReceiptNumber(anyString()))
                .thenReturn(true, false);

        ReceiptNumberService receiptNumberService =
                new ReceiptNumberService(saleRepository);

        String receiptNumber = receiptNumberService.generateUniqueReceiptNumber();

        assertTrue(receiptNumber.matches("REC-\\d{8}-[A-F0-9]{8}"));

        verify(saleRepository, times(2))
                .existsByReceiptNumber(anyString());
    }
}