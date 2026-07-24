package com.possystem.service;

import com.possystem.dto.CreateCashierRequest;
import com.possystem.dto.ResetCashierPasswordRequest;
import com.possystem.dto.UpdateCashierRequest;
import com.possystem.dto.UserResponse;
import com.possystem.dto.UserStatusRequest;
import com.possystem.entity.Role;
import com.possystem.entity.User;
import com.possystem.repository.RoleRepository;
import com.possystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserResponse createCashier(CreateCashierRequest request) {

        String fullName = request.getFullName().trim();
        String username = request.getUsername().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        Role cashierRole = roleRepository.findByName("CASHIER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Cashier role was not found"
                ));

        User cashier = new User(
                fullName,
                username,
                email,
                passwordEncoder.encode(request.getPassword()),
                "ACTIVE",
                cashierRole
        );

        User savedCashier = userRepository.save(cashier);

        auditLogService.record(
                "CASHIER_CREATED",
                "USER",
                savedCashier.getId(),
                "Cashier user created: " + savedCashier.getUsername()
        );

        return mapToUserResponse(savedCashier);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listCashiers() {
        return userRepository.findByRole_Name("CASHIER")
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getCashierById(Long cashierId) {
        User cashier = userRepository
                .findByIdAndRole_Name(cashierId, "CASHIER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cashier was not found"
                ));

        return mapToUserResponse(cashier);
    }

    @Transactional
    public UserResponse updateCashierStatus(
            Long cashierId,
            UserStatusRequest request
    ) {
        User cashier = userRepository
                .findByIdAndRole_Name(cashierId, "CASHIER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cashier was not found"
                ));

        String status = request.getStatus()
                .trim()
                .toUpperCase(Locale.ROOT);

        cashier.setStatus(status);

        User updatedCashier = userRepository.save(cashier);

        auditLogService.record(
                "CASHIER_STATUS_UPDATED",
                "USER",
                updatedCashier.getId(),
                "Cashier status updated to "
                        + updatedCashier.getStatus()
                        + ": "
                        + updatedCashier.getUsername()
        );

        return mapToUserResponse(updatedCashier);
    }

    @Transactional
    public UserResponse resetCashierPassword(
            Long cashierId,
            ResetCashierPasswordRequest request
    ) {
        User cashier = userRepository
                .findByIdAndRole_Name(cashierId, "CASHIER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cashier was not found"
                ));

        cashier.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword())
        );

        User updatedCashier = userRepository.save(cashier);

        auditLogService.record(
                "CASHIER_PASSWORD_RESET",
                "USER",
                updatedCashier.getId(),
                "Cashier password reset: "
                        + updatedCashier.getUsername()
        );

        return mapToUserResponse(updatedCashier);
    }

    @Transactional
    public UserResponse updateCashier(
            Long cashierId,
            UpdateCashierRequest request
    ) {
        User cashier = userRepository
                .findByIdAndRole_Name(cashierId, "CASHIER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cashier was not found"
                ));

        String fullName = request.getFullName().trim();
        String username = request.getUsername().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameAndIdNot(username, cashierId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmailAndIdNot(email, cashierId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        cashier.setFullName(fullName);
        cashier.setUsername(username);
        cashier.setEmail(email);

        User updatedCashier = userRepository.save(cashier);

        auditLogService.record(
                "CASHIER_PROFILE_UPDATED",
                "USER",
                updatedCashier.getId(),
                "Cashier profile updated: "
                        + updatedCashier.getUsername()
        );

        return mapToUserResponse(updatedCashier);
    }
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleName(user.getRole().getName())
                .status(user.getStatus())
                .build();
    }
}
