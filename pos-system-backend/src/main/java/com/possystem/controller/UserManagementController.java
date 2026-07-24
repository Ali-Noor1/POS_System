package com.possystem.controller;

import com.possystem.dto.CreateCashierRequest;
import com.possystem.dto.ResetCashierPasswordRequest;
import com.possystem.dto.UpdateCashierRequest;
import com.possystem.dto.UserResponse;
import com.possystem.dto.UserStatusRequest;
import com.possystem.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(
            UserManagementService userManagementService
    ) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/cashiers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createCashier(
            @Valid @RequestBody CreateCashierRequest request
    ) {
        UserResponse response = userManagementService.createCashier(
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/cashiers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listCashiers() {
        List<UserResponse> response = userManagementService.listCashiers();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cashiers/{cashierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getCashierById(
            @PathVariable Long cashierId
    ) {
        UserResponse response = userManagementService.getCashierById(
                cashierId
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/cashiers/{cashierId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateCashierStatus(
            @PathVariable Long cashierId,
            @Valid @RequestBody UserStatusRequest request
    ) {
        UserResponse response = userManagementService.updateCashierStatus(
                cashierId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/cashiers/{cashierId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> resetCashierPassword(
            @PathVariable Long cashierId,
            @Valid @RequestBody ResetCashierPasswordRequest request
    ) {
        UserResponse response = userManagementService.resetCashierPassword(
                cashierId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/cashiers/{cashierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateCashier(
            @PathVariable Long cashierId,
            @Valid @RequestBody UpdateCashierRequest request
    ) {
        UserResponse response = userManagementService.updateCashier(
                cashierId,
                request
        );

        return ResponseEntity.ok(response);
    }
}
