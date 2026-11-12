package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    public DepartmentService(DepartmentRepository departmentRepository, AuditLogService auditLogService) {
        this.departmentRepository = departmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> list(String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normQuery = (query == null || query.isBlank()) ? null : query.trim();
        return departmentRepository.searchByOrganization(orgId, normQuery, pageable).map(this::toDto);
    }

    @Transactional
    public DepartmentDto create(ZeniusUserPrincipal principal, DepartmentMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String code = req.code().trim().toUpperCase();
        if (departmentRepository.existsByOrganizationIdAndCodeIgnoreCaseAndDeletedAtIsNull(orgId, code)) {
            throw new IllegalStateException("Department code already exists in organization: " + code);
        }

        DepartmentEntity dept = new DepartmentEntity();
        dept.setOrganizationId(orgId);
        dept.setCode(code);
        dept.setName(req.name().trim());
        dept.setDescription(req.description() != null ? req.description().trim() : null);
        dept.setParentDepartmentId(req.parentDepartmentId());
        dept.setStatus("ACTIVE");
        dept = departmentRepository.save(dept);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "DEPARTMENT_CREATED",
                "DEPARTMENT",
                dept.getId().toString(),
                "Created department " + dept.getCode() + " - " + dept.getName(),
                "SUCCESS"
        );
        return toDto(dept);
    }

    @Transactional
    public DepartmentDto update(ZeniusUserPrincipal principal, UUID deptId, DepartmentMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        DepartmentEntity dept = departmentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(deptId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + deptId));
        dept.setName(req.name().trim());
        dept.setDescription(req.description() != null ? req.description().trim() : null);
        dept.setParentDepartmentId(req.parentDepartmentId());
        dept = departmentRepository.save(dept);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "DEPARTMENT_UPDATED",
                "DEPARTMENT",
                dept.getId().toString(),
                "Updated department " + dept.getCode(),
                "SUCCESS"
        );
        return toDto(dept);
    }

    @Transactional
    public void archive(ZeniusUserPrincipal principal, UUID deptId) {
        UUID orgId = TenantContext.requireOrganizationId();
        DepartmentEntity dept = departmentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(deptId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + deptId));
        dept.setDeletedAt(Instant.now());
        dept.setStatus("ARCHIVED");
        departmentRepository.save(dept);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "DEPARTMENT_ARCHIVED",
                "DEPARTMENT",
                dept.getId().toString(),
                "Archived department " + dept.getCode(),
                "SUCCESS"
        );
    }

    private DepartmentDto toDto(DepartmentEntity d) {
        return new DepartmentDto(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getDescription(),
                d.getParentDepartmentId(),
                d.getStatus(),
                d.getCreatedAt()
        );
    }

    public record DepartmentMutationRequest(
            String code,
            String name,
            String description,
            UUID parentDepartmentId
    ) {
    }

    public record DepartmentDto(
            UUID id,
            String code,
            String name,
            String description,
            UUID parentDepartmentId,
            String status,
            Instant createdAt
    ) {
    }
}

