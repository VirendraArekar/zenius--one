package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.UserEntity;
import com.zenius.one.modules.identity.UserRepository;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import com.zenius.one.platform.notifications.NotificationService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final OrgMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public MemberService(
            OrgMembershipRepository membershipRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<MemberDto> listMembers(String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        String normQuery = (query == null || query.isBlank()) ? null : query.trim();
        Page<OrgMembershipEntity> page = membershipRepository.searchMembers(orgId, normQuery, pageable);

        Set<UUID> userIds = page.getContent().stream().map(OrgMembershipEntity::getUserId).collect(Collectors.toSet());
        Map<UUID, UserEntity> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return page.map(m -> toDto(m, usersById.get(m.getUserId())));
    }

    @Transactional
    public MemberDto addOrInviteMember(ZeniusUserPrincipal principal, AddMemberRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String email = req.email().trim().toLowerCase();

        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            if (req.temporaryPassword() == null || req.temporaryPassword().length() < 8) {
                throw new IllegalArgumentException(
                        "Temporary password (min 8 chars) is required when creating a new user account"
                );
            }
            UserEntity created = new UserEntity();
            created.setEmail(email);
            created.setFullName(req.fullName().trim());
            created.setJobTitle(req.designation());
            created.setPasswordHash(passwordEncoder.encode(req.temporaryPassword()));
            created.setStatus("ACTIVE");
            return userRepository.save(created);
        });

        if (membershipRepository.findByOrganizationIdAndUserId(orgId, user.getId()).isPresent()) {
            throw new IllegalStateException("User is already a member of this organization");
        }

        validateOrgReferences(orgId, req.branchId(), req.departmentId());
        Set<RoleEntity> assignedRoles = resolveRoles(orgId, req.roleIds());

        OrgMembershipEntity membership = new OrgMembershipEntity();
        membership.setOrganizationId(orgId);
        membership.setUserId(user.getId());
        membership.setBranchId(req.branchId());
        membership.setDepartmentId(req.departmentId());
        membership.setEmployeeCode(req.employeeCode());
        membership.setDesignation(req.designation());
        membership.setStatus("ACTIVE");
        membership.setRoles(assignedRoles);
        membership = membershipRepository.save(membership);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "MEMBER_ADDED",
                "MEMBER",
                membership.getId().toString(),
                "Added member " + user.getEmail() + " to organization",
                "SUCCESS"
        );

        notificationService.send(
                orgId,
                user.getId(),
                "Added to Organization Workspace",
                "You have been added as a member with designation: "
                        + (req.designation() != null ? req.designation() : "Team Member"),
                "ORGANIZATION",
                "/admin"
        );

        return toDto(membership, user);
    }

    @Transactional
    public MemberDto updateMember(ZeniusUserPrincipal principal, UUID membershipId, UpdateMemberRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        OrgMembershipEntity membership = membershipRepository.findByIdAndOrganizationId(membershipId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + membershipId));

        validateOrgReferences(orgId, req.branchId(), req.departmentId());
        membership.setBranchId(req.branchId());
        membership.setDepartmentId(req.departmentId());
        membership.setEmployeeCode(req.employeeCode());
        membership.setDesignation(req.designation());
        if (req.status() != null && !req.status().isBlank()) {
            membership.setStatus(req.status().trim().toUpperCase());
        }
        if (req.roleIds() != null && !req.roleIds().isEmpty()) {
            membership.setRoles(resolveRoles(orgId, req.roleIds()));
        }
        membership = membershipRepository.save(membership);

        UserEntity user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "MEMBER_UPDATED",
                "MEMBER",
                membership.getId().toString(),
                "Updated member " + user.getEmail(),
                "SUCCESS"
        );
        return toDto(membership, user);
    }

    private void validateOrgReferences(UUID orgId, UUID branchId, UUID departmentId) {
        if (branchId != null && branchRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(branchId, orgId).isEmpty()) {
            throw new IllegalArgumentException("Branch does not belong to current organization");
        }
        if (departmentId != null
                && departmentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(departmentId, orgId).isEmpty()) {
            throw new IllegalArgumentException("Department does not belong to current organization");
        }
    }

    private Set<RoleEntity> resolveRoles(UUID orgId, List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            RoleEntity memberRole = roleRepository.findByOrganizationIdAndCodeIgnoreCase(orgId, "MEMBER")
                    .orElseThrow(() -> new IllegalStateException("Default MEMBER role missing in organization"));
            return new HashSet<>(List.of(memberRole));
        }
        List<RoleEntity> roles = roleRepository.findByOrganizationIdAndIdIn(orgId, roleIds);
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("One or more role IDs do not belong to current organization");
        }
        return new HashSet<>(roles);
    }

    private MemberDto toDto(OrgMembershipEntity m, UserEntity u) {
        List<RoleRef> roles = m.getRoles().stream()
                .map(r -> new RoleRef(r.getId(), r.getCode(), r.getName()))
                .toList();
        return new MemberDto(
                m.getId(),
                m.getUserId(),
                u != null ? u.getEmail() : "",
                u != null ? u.getFullName() : "",
                m.getEmployeeCode(),
                m.getDesignation(),
                m.getBranchId(),
                m.getDepartmentId(),
                m.getStatus(),
                m.getJoinedAt(),
                roles
        );
    }

    public record RoleRef(UUID id, String code, String name) {
    }

    public record MemberDto(
            UUID id,
            UUID userId,
            String email,
            String fullName,
            String employeeCode,
            String designation,
            UUID branchId,
            UUID departmentId,
            String status,
            Instant joinedAt,
            List<RoleRef> roles
    ) {
    }

    public record AddMemberRequest(
            String fullName,
            String email,
            String temporaryPassword,
            String employeeCode,
            String designation,
            UUID branchId,
            UUID departmentId,
            List<UUID> roleIds
    ) {
    }

    public record UpdateMemberRequest(
            String employeeCode,
            String designation,
            UUID branchId,
            UUID departmentId,
            String status,
            List<UUID> roleIds
    ) {
    }
}

