package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.AuthService;
import com.zenius.one.modules.identity.AuthService.AuthSessionResponse;
import com.zenius.one.modules.identity.AuthService.OrganizationSummary;
import com.zenius.one.modules.identity.UserEntity;
import com.zenius.one.modules.identity.UserRepository;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository membershipRepository;
    private final OrganizationProvisioningService provisioningService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrgMembershipRepository membershipRepository,
            OrganizationProvisioningService provisioningService,
            UserRepository userRepository,
            AuthService authService,
            AuditLogService auditLogService
    ) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.provisioningService = provisioningService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationSummary> listMyOrganizations(ZeniusUserPrincipal principal) {
        return organizationRepository.findActiveOrganizationsForUser(principal.getUserId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public OrganizationSummary createOrganization(ZeniusUserPrincipal principal, CreateOrgRequest req) {
        OrganizationEntity org = provisioningService.createOrganizationWithDefaults(
                principal.getUserId(),
                principal.getEmail(),
                req.name(),
                req.slug(),
                req.industry(),
                req.baseCurrency(),
                req.timezone()
        );
        return toSummary(org);
    }

    @Transactional
    public AuthSessionResponse switchOrganization(
            ZeniusUserPrincipal principal,
            UUID targetOrgId,
            String userAgent,
            String ipAddress
    ) {
        OrgMembershipEntity membership = membershipRepository
                .findByOrganizationIdAndUserId(targetOrgId, principal.getUserId())
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .orElseThrow(() -> new AccessDeniedException("You do not belong to organization: " + targetOrgId));

        UserEntity user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditLogService.record(
                membership.getOrganizationId(),
                user.getId(),
                user.getEmail(),
                "ORG_SWITCHED",
                "ORGANIZATION",
                targetOrgId.toString(),
                "Switched active organization context",
                "SUCCESS"
        );

        return authService.issueSession(user, targetOrgId, null, userAgent, ipAddress);
    }

    @Transactional
    public OrganizationSummary updateCurrentOrganization(ZeniusUserPrincipal principal, UpdateOrgRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        org.setName(req.name().trim());
        if (req.industry() != null) {
            org.setIndustry(req.industry().trim());
        }
        if (req.baseCurrency() != null && !req.baseCurrency().isBlank()) {
            org.setBaseCurrency(req.baseCurrency().trim().toUpperCase());
        }
        if (req.timezone() != null && !req.timezone().isBlank()) {
            org.setTimezone(req.timezone().trim());
        }
        org = organizationRepository.save(org);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "ORG_UPDATED",
                "ORGANIZATION",
                orgId.toString(),
                "Updated organization profile for " + org.getName(),
                "SUCCESS"
        );
        return toSummary(org);
    }

    private OrganizationSummary toSummary(OrganizationEntity o) {
        return new OrganizationSummary(
                o.getId(),
                o.getSlug(),
                o.getName(),
                o.getIndustry(),
                o.getBaseCurrency(),
                o.getTimezone()
        );
    }

    public record CreateOrgRequest(
            String name,
            String slug,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }

    public record UpdateOrgRequest(
            String name,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }
}

