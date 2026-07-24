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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(
                userRepository,
                roleRepository,
                passwordEncoder,
                auditLogService
        );
    }

    @Test
    void createCashier_createsActiveCashierWithEncodedPasswordAndSafeResponse() {

        CreateCashierRequest request = validRequest();

        Role cashierRole = new Role("CASHIER", "Cashier role");
        ReflectionTestUtils.setField(cashierRole, "id", 2L);

        when(userRepository.existsByUsername("cashier_one"))
                .thenReturn(false);
        when(userRepository.existsByEmail("cashier.one@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("CASHIER"))
                .thenReturn(Optional.of(cashierRole));
        when(passwordEncoder.encode("Cashier@123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(10L);
                    return savedUser;
                });

        UserResponse response = userManagementService.createCashier(request);

        assertEquals(10L, response.getId());
        assertEquals("Cashier One", response.getFullName());
        assertEquals("cashier_one", response.getUsername());
        assertEquals("cashier.one@example.com", response.getEmail());
        assertEquals("CASHIER", response.getRoleName());
        assertEquals("ACTIVE", response.getStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Cashier One", savedUser.getFullName());
        assertEquals("cashier_one", savedUser.getUsername());
        assertEquals("cashier.one@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertNotEquals("Cashier@123", savedUser.getPasswordHash());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertEquals(cashierRole, savedUser.getRole());
        verify(auditLogService).record(
                "CASHIER_CREATED",
                "USER",
                10L,
                "Cashier user created: cashier_one"
        );
    }

    @Test
    void createCashier_throwsConflict_whenUsernameAlreadyExists() {

        CreateCashierRequest request = validRequest();

        when(userRepository.existsByUsername("cashier_one"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.createCashier(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createCashier_throwsConflict_whenEmailAlreadyExists() {

        CreateCashierRequest request = validRequest();

        when(userRepository.existsByUsername("cashier_one"))
                .thenReturn(false);
        when(userRepository.existsByEmail("cashier.one@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.createCashier(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createCashier_throwsInternalServerError_whenCashierRoleIsMissing() {

        CreateCashierRequest request = validRequest();

        when(userRepository.existsByUsername("cashier_one"))
                .thenReturn(false);
        when(userRepository.existsByEmail("cashier.one@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("CASHIER"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.createCashier(request)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Cashier role was not found", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listCashiers_returnsOnlyCashiersAsSafeResponses() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        ReflectionTestUtils.setField(cashierRole, "id", 2L);

        User activeCashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password-one",
                "ACTIVE",
                cashierRole
        );
        activeCashier.setId(10L);

        User inactiveCashier = new User(
                "Cashier Two",
                "cashier_two",
                "cashier.two@example.com",
                "encoded-password-two",
                "INACTIVE",
                cashierRole
        );
        inactiveCashier.setId(11L);

        when(userRepository.findByRole_Name("CASHIER"))
                .thenReturn(List.of(activeCashier, inactiveCashier));

        List<UserResponse> response = userManagementService.listCashiers();

        assertEquals(2, response.size());

        UserResponse firstCashier = response.get(0);
        assertEquals(10L, firstCashier.getId());
        assertEquals("Cashier One", firstCashier.getFullName());
        assertEquals("cashier_one", firstCashier.getUsername());
        assertEquals("cashier.one@example.com", firstCashier.getEmail());
        assertEquals("CASHIER", firstCashier.getRoleName());
        assertEquals("ACTIVE", firstCashier.getStatus());

        UserResponse secondCashier = response.get(1);
        assertEquals(11L, secondCashier.getId());
        assertEquals("Cashier Two", secondCashier.getFullName());
        assertEquals("cashier_two", secondCashier.getUsername());
        assertEquals("cashier.two@example.com", secondCashier.getEmail());
        assertEquals("CASHIER", secondCashier.getRoleName());
        assertEquals("INACTIVE", secondCashier.getStatus());

        verify(userRepository).findByRole_Name("CASHIER");
    }

    @Test
    void getCashierById_returnsCashierAsSafeResponse() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        ReflectionTestUtils.setField(cashierRole, "id", 2L);

        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        when(userRepository.findByIdAndRole_Name(10L, "CASHIER"))
                .thenReturn(Optional.of(cashier));

        UserResponse response = userManagementService.getCashierById(10L);

        assertEquals(10L, response.getId());
        assertEquals("Cashier One", response.getFullName());
        assertEquals("cashier_one", response.getUsername());
        assertEquals("cashier.one@example.com", response.getEmail());
        assertEquals("CASHIER", response.getRoleName());
        assertEquals("ACTIVE", response.getStatus());

        verify(userRepository).findByIdAndRole_Name(10L, "CASHIER");
    }

    @Test
    void updateCashierStatus_updatesStatusAndReturnsSafeResponse() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        ReflectionTestUtils.setField(cashierRole, "id", 2L);

        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        UserStatusRequest request = new UserStatusRequest();
        request.setStatus(" inactive ");

        when(userRepository.findByIdAndRole_Name(10L, "CASHIER"))
                .thenReturn(Optional.of(cashier));
        when(userRepository.save(cashier))
                .thenReturn(cashier);

        UserResponse response = userManagementService
                .updateCashierStatus(10L, request);

        assertEquals(10L, response.getId());
        assertEquals("Cashier One", response.getFullName());
        assertEquals("cashier_one", response.getUsername());
        assertEquals("cashier.one@example.com", response.getEmail());
        assertEquals("CASHIER", response.getRoleName());
        assertEquals("INACTIVE", response.getStatus());
        assertEquals("INACTIVE", cashier.getStatus());

        verify(userRepository).findByIdAndRole_Name(10L, "CASHIER");
        verify(userRepository).save(cashier);
        verify(auditLogService).record(
                "CASHIER_STATUS_UPDATED",
                "USER",
                10L,
                "Cashier status updated to INACTIVE: cashier_one"
        );
    }

    @Test
    void resetCashierPassword_encodesPasswordAndReturnsSafeResponse() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        ReflectionTestUtils.setField(cashierRole, "id", 2L);

        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "old-encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        ResetCashierPasswordRequest request =
                new ResetCashierPasswordRequest();
        request.setNewPassword("NewCashier@123");

        when(userRepository.findByIdAndRole_Name(10L, "CASHIER"))
                .thenReturn(Optional.of(cashier));
        when(passwordEncoder.encode("NewCashier@123"))
                .thenReturn("new-encoded-password");
        when(userRepository.save(cashier))
                .thenReturn(cashier);

        UserResponse response = userManagementService
                .resetCashierPassword(10L, request);

        assertEquals(10L, response.getId());
        assertEquals("Cashier One", response.getFullName());
        assertEquals("cashier_one", response.getUsername());
        assertEquals("cashier.one@example.com", response.getEmail());
        assertEquals("CASHIER", response.getRoleName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("new-encoded-password", cashier.getPasswordHash());
        assertNotEquals("NewCashier@123", cashier.getPasswordHash());

        verify(userRepository).findByIdAndRole_Name(10L, "CASHIER");
        verify(passwordEncoder).encode("NewCashier@123");
        verify(userRepository).save(cashier);
        verify(auditLogService).record(
                "CASHIER_PASSWORD_RESET",
                "USER",
                10L,
                "Cashier password reset: cashier_one"
        );
    }

    @Test
    void updateCashier_updatesProfileAndReturnsSafeResponse() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        ReflectionTestUtils.setField(cashierRole, "id", 2L);

        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        UpdateCashierRequest request = validUpdateRequest();

        when(userRepository.findByIdAndRole_Name(10L, "CASHIER"))
                .thenReturn(Optional.of(cashier));
        when(userRepository.existsByUsernameAndIdNot(
                "updated_cashier",
                10L
        )).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot(
                "updated.cashier@example.com",
                10L
        )).thenReturn(false);
        when(userRepository.save(cashier))
                .thenReturn(cashier);

        UserResponse response = userManagementService
                .updateCashier(10L, request);

        assertEquals(10L, response.getId());
        assertEquals("Updated Cashier", response.getFullName());
        assertEquals("updated_cashier", response.getUsername());
        assertEquals("updated.cashier@example.com", response.getEmail());
        assertEquals("CASHIER", response.getRoleName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("encoded-password", cashier.getPasswordHash());

        verify(userRepository).findByIdAndRole_Name(10L, "CASHIER");
        verify(userRepository).existsByUsernameAndIdNot(
                "updated_cashier",
                10L
        );
        verify(userRepository).existsByEmailAndIdNot(
                "updated.cashier@example.com",
                10L
        );
        verify(userRepository).save(cashier);
        verify(auditLogService).record(
                "CASHIER_PROFILE_UPDATED",
                "USER",
                10L,
                "Cashier profile updated: updated_cashier"
        );
    }

    @Test
    void updateCashier_throwsConflict_whenUsernameBelongsToAnotherUser() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        UpdateCashierRequest request = validUpdateRequest();

        when(userRepository.findByIdAndRole_Name(10L, "CASHIER"))
                .thenReturn(Optional.of(cashier));
        when(userRepository.existsByUsernameAndIdNot(
                "updated_cashier",
                10L
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.updateCashier(10L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateCashier_throwsConflict_whenEmailBelongsToAnotherUser() {

        Role cashierRole = new Role("CASHIER", "Cashier role");
        User cashier = new User(
                "Cashier One",
                "cashier_one",
                "cashier.one@example.com",
                "encoded-password",
                "ACTIVE",
                cashierRole
        );
        cashier.setId(10L);

        UpdateCashierRequest request = validUpdateRequest();

        when(userRepository.findByIdAndRole_Name(10L, "CASHIER"))
                .thenReturn(Optional.of(cashier));
        when(userRepository.existsByUsernameAndIdNot(
                "updated_cashier",
                10L
        )).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot(
                "updated.cashier@example.com",
                10L
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.updateCashier(10L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    private CreateCashierRequest validRequest() {
        CreateCashierRequest request = new CreateCashierRequest();
        request.setFullName("  Cashier One  ");
        request.setUsername("  cashier_one  ");
        request.setEmail("  Cashier.One@Example.COM  ");
        request.setPassword("Cashier@123");
        return request;
    }

    private UpdateCashierRequest validUpdateRequest() {
        UpdateCashierRequest request = new UpdateCashierRequest();
        request.setFullName("  Updated Cashier  ");
        request.setUsername("  updated_cashier  ");
        request.setEmail("  Updated.Cashier@Example.COM  ");
        return request;
    }
}
