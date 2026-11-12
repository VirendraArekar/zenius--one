package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            AuditLogService auditLogService
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> listAllPermissions() {
        return permissionRepository.findAllByOrderByModuleCodeAscPermissionKeyAsc().stream()
                .map(p -> new PermissionDto(p.getId(), p.getModuleCode(), p.getPermissionKey(), p.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        UUID orgId = TenantContext.requireOrganizationId();
        return roleRepository.findByOrganizationIdOrderBySystemRoleDescNameAsc(orgId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RoleDto createCustomRole(ZeniusUserPrincipal principal, RoleMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String code = req.code().trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
        if (roleRepository.existsByOrganizationIdAndCodeIgnoreCase(orgId, code)) {
            throw new IllegalStateException("Role code already exists in organization: " + code);
        }

        Set<PermissionEntity> permissions = resolvePermissions(req.permissionKeys());
        RoleEntity role = new RoleEntity();
        role.setOrganizationId(orgId);
        role.setCode(code);
        role.setName(req.name().trim());
        role.setDescription(req.description() != null ? req.description().trim() : null);
        role.setSystemRole(false);
        role.setPermissions(permissions);
        role = roleRepository.save(role);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "ROLE_CREATED",
                "ROLE",
                role.getId().toString(),
                "Created custom role " + role.getCode(),
                "SUCCESS"
        );
        return toDto(role);
    }

    @Transactional
    public RoleDto updateRolePermissions(ZeniusUserPrincipal principal, UUID roleId, RoleMutationRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        RoleEntity role = roleRepository.findByIdAndOrganizationId(roleId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        if ("ORG_OWNER".equals(role.getCode())) {
            throw new IllegalStateException("ORG_OWNER permissions cannot be restricted");
        }

        role.setName(req.name().trim());
        role.setDescription(req.description() != null ? req.description().trim() : null);
        role.setPermissions(resolvePermissions(req.permissionKeys()));
        role = roleRepository.save(role);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "ROLE_UPDATED",
                "ROLE",
                role.getId().toString(),
                "Updated permissions on role " + role.getCode(),
                "SUCCESS"
        );
        return toDto(role);
    }

    private Set<PermissionEntity> resolvePermissions(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashSet<>();
        }
        List<PermissionEntity> found = permissionRepository.findByPermissionKeyIn(keys);
        if (found.size() != new HashSet<>(keys).size()) {
            throw new IllegalArgumentException("One or more permission keys are invalid");
        }
        return new HashSet<>(found);
    }

    private RoleDto toDto(RoleEntity r) {
        List<String> perms = r.getPermissions().stream()
                .map(PermissionEntity::getPermissionKey)
                .sorted()
                .toList();
        return new RoleDto(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isSystemRole(), perms);
    }

    public record PermissionDto(UUID id, String moduleCode, String permissionKey, String description) {
    }

    public record RoleDto(
            UUID id,
            String code,
            String name,
            String description,
            boolean systemRole,
            List<String> permissionKeys
    ) {
    }

    public record RoleMutationRequest(
            String code,
            String name,
            String description,
            List<String> permissionKeys
    ) {
    }
}

