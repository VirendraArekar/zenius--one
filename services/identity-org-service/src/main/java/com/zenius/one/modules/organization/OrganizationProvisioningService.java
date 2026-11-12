package com.zenius.one.modules.organization;

import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.notifications.NotificationService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions a new organization with default system roles, HQ branch, default department, and owner membership.
 */
@Service
public class OrganizationProvisioningService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final OrgMembershipRepository membershipRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public OrganizationProvisioningService(
            OrganizationRepository organizationRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            OrgMembershipRepository membershipRepository,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrganizationEntity createOrganizationWithDefaults(
            UUID ownerUserId,
            String ownerEmail,
            String name,
            String slug,
            String industry,
            String baseCurrency,
            String timezone
    ) {
        String normalizedSlug = normalizeSlug(slug, name);
        if (organizationRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new IllegalStateException("Organization slug already exists: " + normalizedSlug);
        }

        OrganizationEntity org = new OrganizationEntity();
        org.setName(name.trim());
        org.setSlug(normalizedSlug);
        org.setIndustry(industry != null && !industry.isBlank() ? industry.trim() : "Technology");
        org.setBaseCurrency(baseCurrency != null && !baseCurrency.isBlank() ? baseCurrency.trim().toUpperCase() : "USD");
        org.setTimezone(timezone != null && !timezone.isBlank() ? timezone.trim() : "UTC");
        org.setStatus("ACTIVE");
        org = organizationRepository.save(org);

        BranchEntity hq = new BranchEntity();
        hq.setOrganizationId(org.getId());
        hq.setCode("HQ");
        hq.setName("Headquarters");
        hq.setHeadquarters(true);
        hq.setStatus("ACTIVE");
        hq = branchRepository.save(hq);

        DepartmentEntity generalDept = new DepartmentEntity();
        generalDept.setOrganizationId(org.getId());
        generalDept.setCode("EXEC");
        generalDept.setName("Executive & Operations");
        generalDept.setDescription("Default leadership and operations department");
        generalDept.setStatus("ACTIVE");
        generalDept = departmentRepository.save(generalDept);

        List<PermissionEntity> allPermissions = permissionRepository.findAllByOrderByModuleCodeAscPermissionKeyAsc();

        RoleEntity ownerRole = createSystemRole(
                org.getId(),
                "ORG_OWNER",
                "Organization Owner",
                "Full administrative control across organization modules and billing",
                new HashSet<>(allPermissions)
        );

        createSystemRole(
                org.getId(),
                "ORG_ADMIN",
                "Organization Admin",
                "Manage users, departments, branches, roles, and security",
                new HashSet<>(allPermissions)
        );

        Set<PermissionEntity> memberPerms = new HashSet<>(
                permissionRepository.findByPermissionKeyIn(List.of(
                        "org:read", "branch:read", "department:read", "member:read", "notification:read",
                        "crm:lead:read", "crm:contact:read", "crm:account:read", "crm:deal:read",
                        "crm:product:read", "crm:quote:read", "crm:activity:read",
                        "marketing:read", "support:ticket:read", "support:kb:read", "support:field:read",
                        "finance:read", "billing:read", "inventory:read",
                        "hr:read", "recruitment:read",
                        "projects:read", "collab:read", "docs:read", "legal:read",
                        "analytics:read", "creator:read", "automation:read", "ai:read"
                ))
        );
        createSystemRole(
                org.getId(),
                "MEMBER",
                "Standard Member",
                "Standard organization employee access",
                memberPerms
        );

        Set<PermissionEntity> auditorPerms = new HashSet<>(
                permissionRepository.findByPermissionKeyIn(List.of(
                        "org:read", "branch:read", "department:read", "member:read", "role:read", "audit:read", "notification:read"
                ))
        );
        createSystemRole(
                org.getId(),
                "AUDITOR",
                "Security Auditor",
                "Read-only compliance and audit log visibility",
                auditorPerms
        );

        OrgMembershipEntity membership = new OrgMembershipEntity();
        membership.setOrganizationId(org.getId());
        membership.setUserId(ownerUserId);
        membership.setBranchId(hq.getId());
        membership.setDepartmentId(generalDept.getId());
        membership.setEmployeeCode("EMP-001");
        membership.setDesignation("Organization Owner");
        membership.setStatus("ACTIVE");
        membership.getRoles().add(ownerRole);
        membershipRepository.save(membership);

        auditLogService.record(
                org.getId(),
                ownerUserId,
                ownerEmail,
                "ORG_CREATED",
                "ORGANIZATION",
                org.getId().toString(),
                "Provisioned organization " + org.getName() + " (" + org.getSlug() + ")",
                "SUCCESS"
        );

        notificationService.send(
                org.getId(),
                ownerUserId,
                "Welcome to " + org.getName(),
                "Your organization workspace, default HQ branch, and RBAC roles have been provisioned.",
                "ORGANIZATION",
                "/admin"
        );

        return org;
    }

    private RoleEntity createSystemRole(
            UUID orgId,
            String code,
            String name,
            String description,
            Set<PermissionEntity> permissions
    ) {
        RoleEntity role = new RoleEntity();
        role.setOrganizationId(orgId);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setSystemRole(true);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    private String normalizeSlug(String slug, String name) {
        String source = (slug != null && !slug.isBlank()) ? slug : name;
        String normalized = source.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("Organization slug must contain at least 2 alphanumeric characters");
        }
        return normalized;
    }
}

