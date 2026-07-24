package com.possystem.service;

import com.possystem.entity.AuditLog;
import com.possystem.entity.Role;
import com.possystem.entity.User;
import com.possystem.repository.AuditLogRepository;
import com.possystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_savesAuditLogWithAuthenticatedActor() {

        AuditLogService auditLogService = new AuditLogService(
                auditLogRepository,
                userRepository
        );
        User admin = adminUser();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        auditLogService.record(
                "CASHIER_CREATED",
                "USER",
                10L,
                "Cashier user created: cashier_one"
        );

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertSame(admin, savedLog.getActor());
        assertEquals("admin", savedLog.getActorUsername());
        assertEquals("CASHIER_CREATED", savedLog.getAction());
        assertEquals("USER", savedLog.getEntityType());
        assertEquals(10L, savedLog.getEntityId());
        assertEquals(
                "Cashier user created: cashier_one",
                savedLog.getMessage()
        );
    }

    private User adminUser() {
        Role role = new Role("ADMIN", "Admin role");
        User user = new User(
                "Admin User",
                "admin",
                "admin@example.com",
                "encoded-password",
                "ACTIVE",
                role
        );
        user.setId(1L);
        return user;
    }
}
