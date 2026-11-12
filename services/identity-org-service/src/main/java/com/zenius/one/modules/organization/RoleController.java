package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.organization.RoleService.PermissionDto;
import com.zenius.one.modules.organization.RoleService.RoleDto;
import com.zenius.one.modules.organization.RoleService.RoleMutationRequest;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Roles & Permissions", description = "Fine-grained RBAC roles and permission catalog")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/permissions")
    @PreAuthorize("@authz.hasPermission('role:read')")
    @Operation(summary = "List all platform RBAC permission keys")
    public ApiResponse<List<PermissionDto>> listPermissions() {
        return ApiResponse.ok(roleService.listAllPermissions());
    }

    @GetMapping("/roles")
    @PreAuthorize("@authz.hasPermission('role:read')")
    @Operation(summary = "List roles in the current organization")
    public ApiResponse<List<RoleDto>> listRoles() {
        return ApiResponse.ok(roleService.listRoles());
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('role:write')")
    @Operation(summary = "Create a custom organization role")
    public ApiResponse<RoleDto> createRole(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody RoleBody body
    ) {
        return ApiResponse.ok(roleService.createCustomRole(
                principal,
                new RoleMutationRequest(body.code(), body.name(), body.description(), body.permissionKeys())
        ));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("@authz.hasPermission('role:write')")
    @Operation(summary = "Update a role's name, description, and permission matrix")
    public ApiResponse<RoleDto> updateRole(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RoleBody body
    ) {
        return ApiResponse.ok(roleService.updateRolePermissions(
                principal,
                id,
                new RoleMutationRequest(body.code(), body.name(), body.description(), body.permissionKeys())
        ));
    }

    public record RoleBody(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 120) String name,
            String description,
            List<String> permissionKeys
    ) {
    }
}

