package com.possystem.controller;

import com.possystem.dto.PosProductResponse;
import com.possystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
public class PosProductController {

    private final ProductService productService;

    @GetMapping
    public List<PosProductResponse> getProducts() {
        return productService.getActiveProductsForPos();
    }

    @GetMapping("/search")
    public List<PosProductResponse> searchProducts(
            @RequestParam String query
    ) {
        return productService.searchActiveProductsForPos(query);
    }

    @GetMapping("/barcode/{barcode}")
    public PosProductResponse getProductByBarcode(
            @PathVariable String barcode
    ) {
        return productService.getActiveProductForPosByBarcode(barcode);
    }
}
