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
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public BranchService(BranchRepository branchRepository, AuditLogService auditLogService) {
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<BranchDto> list(String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normQuery = (query == null || query.isBlank()) ? null : query.trim();
        return branchRepository.searchByOrganization(orgId, normQuery, pageable).map(this::toDto);
    }

    @Transactional
    public BranchDto create(ZeniusUserPrincipal principal, BranchMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String code = req.code().trim().toUpperCase();
        if (branchRepository.existsByOrganizationIdAndCodeIgnoreCaseAndDeletedAtIsNull(orgId, code)) {
            throw new IllegalStateException("Branch code already exists in organization: " + code);
        }

        BranchEntity branch = new BranchEntity();
        branch.setOrganizationId(orgId);
        branch.setCode(code);
        branch.setName(req.name().trim());
        branch.setCity(trimOrNull(req.city()));
        branch.setCountry(trimOrNull(req.country()));
        branch.setAddress(trimOrNull(req.address()));
        branch.setHeadquarters(req.headquarters());
        branch.setStatus("ACTIVE");
        branch = branchRepository.save(branch);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "BRANCH_CREATED",
                "BRANCH",
                branch.getId().toString(),
                "Created branch " + branch.getCode() + " - " + branch.getName(),
                "SUCCESS"
        );
        return toDto(branch);
    }

    @Transactional
    public BranchDto update(ZeniusUserPrincipal principal, UUID branchId, BranchMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        BranchEntity branch = branchRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(branchId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        branch.setName(req.name().trim());
        branch.setCity(trimOrNull(req.city()));
        branch.setCountry(trimOrNull(req.country()));
        branch.setAddress(trimOrNull(req.address()));
        branch.setHeadquarters(req.headquarters());
        branch = branchRepository.save(branch);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "BRANCH_UPDATED",
                "BRANCH",
                branch.getId().toString(),
                "Updated branch " + branch.getCode(),
                "SUCCESS"
        );
        return toDto(branch);
    }

    @Transactional
    public void archive(ZeniusUserPrincipal principal, UUID branchId) {
        UUID orgId = TenantContext.requireOrganizationId();
        BranchEntity branch = branchRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(branchId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        branch.setDeletedAt(Instant.now());
        branch.setStatus("ARCHIVED");
        branchRepository.save(branch);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "BRANCH_ARCHIVED",
                "BRANCH",
                branch.getId().toString(),
                "Archived branch " + branch.getCode(),
                "SUCCESS"
        );
    }

    private BranchDto toDto(BranchEntity b) {
        return new BranchDto(
                b.getId(),
                b.getCode(),
                b.getName(),
                b.getCity(),
                b.getCountry(),
                b.getAddress(),
                b.isHeadquarters(),
                b.getStatus(),
                b.getCreatedAt()
        );
    }

    private String trimOrNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public record BranchMutationRequest(
            String code,
            String name,
            String city,
            String country,
            String address,
            boolean headquarters
    ) {
    }

    public record BranchDto(
            UUID id,
            String code,
            String name,
            String city,
            String country,
            String address,
            boolean headquarters,
            String status,
            Instant createdAt
    ) {
    }
}

