package com.possystem.service;

import com.possystem.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReceiptNumberService {

    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private static final DateTimeFormatter RECEIPT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SaleRepository saleRepository;

    public ReceiptNumberService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public String generateUniqueReceiptNumber() {

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {

            String datePart = LocalDate.now().format(RECEIPT_DATE_FORMAT);

            String uniquePart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);

            String receiptNumber = "REC-" + datePart + "-" + uniquePart;

            if (!saleRepository.existsByReceiptNumber(receiptNumber)) {
                return receiptNumber;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique receipt number after "
                        + MAX_GENERATION_ATTEMPTS + " attempts"
        );
    }
}