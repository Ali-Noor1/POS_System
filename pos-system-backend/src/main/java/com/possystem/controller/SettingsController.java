package com.possystem.controller;

import com.possystem.dto.AppSettingsResponse;
import com.possystem.dto.ChangePasswordRequest;
import com.possystem.dto.ReceiptSettingsRequest;
import com.possystem.dto.ReceiptSettingsResponse;
import com.possystem.dto.StoreSettingsRequest;
import com.possystem.dto.StoreSettingsResponse;
import com.possystem.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public AppSettingsResponse getSettings() {
        return settingsService.getSettings();
    }

    @PutMapping("/store")
    @PreAuthorize("hasRole('ADMIN')")
    public StoreSettingsResponse updateStoreSettings(
            @Valid @RequestBody StoreSettingsRequest request
    ) {
        return settingsService.updateStoreSettings(request);
    }

    @PostMapping(
            value = "/store/logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public StoreSettingsResponse uploadStoreLogo(
            @RequestParam("image") MultipartFile image
    ) {
        return settingsService.uploadStoreLogo(image);
    }

    @PutMapping("/receipt")
    @PreAuthorize("hasRole('ADMIN')")
    public ReceiptSettingsResponse updateReceiptSettings(
            @Valid @RequestBody ReceiptSettingsRequest request
    ) {
        return settingsService.updateReceiptSettings(request);
    }

    @PatchMapping("/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal
    ) {
        settingsService.changePassword(request, principal);
        return ResponseEntity.noContent().build();
    }
}
