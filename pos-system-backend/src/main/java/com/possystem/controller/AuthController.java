package com.possystem.controller;

import com.possystem.dto.LoginRequest;
import com.possystem.dto.LoginResponse;
import com.possystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(response);
    }
}