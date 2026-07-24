package com.possystem.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class AccessTestController {

    @GetMapping("/authenticated")
    public Map<String, String> authenticatedUser(Authentication authentication) {
        return Map.of(
                "message", "JWT authentication successful",
                "username", authentication.getName()
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminOnly() {
        return Map.of(
                "message", "Admin access granted"
        );
    }

    @GetMapping("/pos")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public Map<String, String> posAccess() {
        return Map.of(
                "message", "POS access granted"
        );
    }
}