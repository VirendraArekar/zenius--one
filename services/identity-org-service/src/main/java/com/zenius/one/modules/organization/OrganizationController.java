package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.AuthService.AuthSessionResponse;
import com.zenius.one.modules.identity.AuthService.OrganizationSummary;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.organization.OrganizationService.CreateOrgRequest;
import com.zenius.one.modules.organization.OrganizationService.UpdateOrgRequest;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Multi-organization lifecycle and active tenant switching")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @Operation(summary = "List organizations the authenticated user belongs to")
    public ApiResponse<List<OrganizationSummary>> listMyOrganizations(
            @AuthenticationPrincipal ZeniusUserPrincipal principal
    ) {
        return ApiResponse.ok(organizationService.listMyOrganizations(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new organization and provision default roles and HQ")
    public ApiResponse<OrganizationSummary> createOrganization(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody CreateOrgBody body
    ) {
        return ApiResponse.ok(organizationService.createOrganization(
                principal,
                new CreateOrgRequest(body.name(), body.slug(), body.industry(), body.baseCurrency(), body.timezone())
        ));
    }

    @PostMapping("/{id}/switch")
    @Operation(summary = "Switch active organization context and receive a scoped Access JWT")
    public ApiResponse<AuthSessionResponse> switchOrganization(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(organizationService.switchOrganization(
                principal,
                id,
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        ));
    }

    @PutMapping("/current")
    @PreAuthorize("@authz.hasPermission('org:write')")
    @Operation(summary = "Update settings for the currently active organization")
    public ApiResponse<OrganizationSummary> updateCurrent(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody UpdateOrgBody body
    ) {
        return ApiResponse.ok(organizationService.updateCurrentOrganization(
                principal,
                new UpdateOrgRequest(body.name(), body.industry(), body.baseCurrency(), body.timezone())
        ));
    }

    public record CreateOrgBody(
            @NotBlank @Size(min = 2, max = 150) String name,
            String slug,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }

    public record UpdateOrgBody(
            @NotBlank @Size(min = 2, max = 150) String name,
            String industry,
            String baseCurrency,
            String timezone
    ) {
    }
}

