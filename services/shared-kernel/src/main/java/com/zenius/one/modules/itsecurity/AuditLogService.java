package com.zenius.one.modules.itsecurity;

import com.zenius.one.platform.common.TenantContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            String action,
            String resourceType,
            String resourceId,
            String summary,
            String status
    ) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setOrganizationId(organizationId);
        entry.setActorUserId(actorUserId);
        entry.setActorEmail(actorEmail);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setSummary(summary);
        entry.setStatus(status != null ? status : "SUCCESS");
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> listLogs(String action, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normAction = (action == null || action.isBlank()) ? null : action.trim();
        String normQuery = (query == null || query.isBlank()) ? null : query.trim();
        return auditLogRepository.searchByOrganization(orgId, normAction, normQuery, pageable)
                .map(a -> new AuditLogDto(
                        a.getId(),
                        a.getOrganizationId(),
                        a.getActorUserId(),
                        a.getActorEmail(),
                        a.getAction(),
                        a.getResourceType(),
                        a.getResourceId(),
                        a.getSummary(),
                        a.getStatus(),
                        a.getCreatedAt()
                ));
    }

    public record AuditLogDto(
            UUID id,
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            String action,
            String resourceType,
            String resourceId,
            String summary,
            String status,
            Instant createdAt
    ) {
    }
}

