package com.yep.service;

import org.springframework.stereotype.Service;

import com.yep.domain.user.User;
import com.yep.repository.AuditLogRepository;
import com.yep.domain.platform.AuditLog;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(User actor, String action, String entity, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(actor, action, entity, entityId, details));
    }
}
