package com.possystem.service;

import com.possystem.dto.LoginRequest;
import com.possystem.dto.LoginResponse;
import com.possystem.entity.User;
import com.possystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {

        User user = userRepository
                .findByUsername(loginRequest.getUsername().trim())
                .orElseThrow(this::invalidCredentials);

        boolean isPasswordCorrect = passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPasswordHash()
        );

        boolean isUserActive = "ACTIVE".equalsIgnoreCase(user.getStatus());

        if (!isPasswordCorrect || !isUserActive) {
            throw invalidCredentials();
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getRole().getName(),
                token,
                "Bearer",
                "Login successful"
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password."
        );
    }
}