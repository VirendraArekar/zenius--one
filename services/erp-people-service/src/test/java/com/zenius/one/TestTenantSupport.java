package com.zenius.one;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenius.one.modules.identity.UserEntity;
import com.zenius.one.modules.identity.UserRepository;
import com.zenius.one.modules.identity.security.JwtTokenService;
import com.zenius.one.modules.organization.OrgMembershipEntity;
import com.zenius.one.modules.organization.OrgMembershipRepository;
import com.zenius.one.modules.organization.OrganizationEntity;
import com.zenius.one.modules.organization.OrganizationRepository;
import com.zenius.one.modules.organization.PermissionRepository;
import com.zenius.one.modules.organization.RoleEntity;
import com.zenius.one.modules.organization.RoleRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestTenantSupport {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrgMembershipRepository membershipRepository;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public TestTenantSupport(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            OrgMembershipRepository membershipRepository,
            JwtTokenService jwtTokenService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.membershipRepository = membershipRepository;
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JsonNode provisionTenant(String fullName, String email, String orgName) {
        UserEntity user = new UserEntity();
        user.setFullName(fullName);
        user.setEmail(email.toLowerCase());
        user.setPasswordHash("$2a$10$dummyhashfortestingonly000000000000000000000000000");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        OrganizationEntity org = new OrganizationEntity();
        org.setName(orgName);
        org.setSlug((orgName.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + UUID.randomUUID().toString().substring(0, 6)));
        org.setBaseCurrency("USD");
        org.setTimezone("UTC");
        org.setStatus("ACTIVE");
        org = organizationRepository.save(org);

        RoleEntity ownerRole = new RoleEntity();
        ownerRole.setOrganizationId(org.getId());
        ownerRole.setCode("ORG_OWNER");
        ownerRole.setName("Organization Owner");
        ownerRole.setSystemRole(true);
        ownerRole.setPermissions(new HashSet<>(permissionRepository.findAll()));
        ownerRole = roleRepository.save(ownerRole);

        OrgMembershipEntity membership = new OrgMembershipEntity();
        membership.setOrganizationId(org.getId());
        membership.setUserId(user.getId());
        membership.setStatus("ACTIVE");
        membership.getRoles().add(ownerRole);
        membershipRepository.save(membership);

        String token = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), org.getId());
        return objectMapper.valueToTree(Map.of(
                "accessToken", token,
                "user", Map.of("id", user.getId().toString(), "activeOrganizationId", org.getId().toString())
        ));
    }
}
