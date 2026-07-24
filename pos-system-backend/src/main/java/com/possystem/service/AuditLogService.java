package com.possystem.service;

import com.possystem.entity.AuditLog;
import com.possystem.entity.User;
import com.possystem.repository.AuditLogRepository;
import com.possystem.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(
            String action,
            String entityType,
            Long entityId,
            String message
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setMessage(message);

        String actorUsername = getCurrentUsername();
        auditLog.setActorUsername(actorUsername);

        if (actorUsername != null) {
            userRepository.findByUsername(actorUsername)
                    .ifPresent(auditLog::setActor);
        }

        auditLogRepository.save(auditLog);
    }

    private String getCurrentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return null;
        }

        return authentication.getName();
    }
}
