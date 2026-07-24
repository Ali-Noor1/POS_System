package com.possystem.controller;

import com.possystem.dto.CancelSaleRequest;
import com.possystem.dto.CompleteSaleResponse;
import com.possystem.entity.SaleStatus;
import com.possystem.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

    @Mock
    private SaleService saleService;

    @Test
    void cancelSale_returnsOkWithUpdatedReceipt() {

        SaleController saleController = new SaleController(saleService);

        CancelSaleRequest request = new CancelSaleRequest();
        request.setCancellationReason("Customer changed their mind");

        CompleteSaleResponse serviceResponse = new CompleteSaleResponse();
        serviceResponse.setSaleId(100L);
        serviceResponse.setSaleStatus(SaleStatus.CANCELLED);

        when(saleService.cancelSale(100L, request))
                .thenReturn(serviceResponse);

        ResponseEntity<CompleteSaleResponse> response =
                saleController.cancelSale(100L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());

        verify(saleService).cancelSale(100L, request);
    }
}