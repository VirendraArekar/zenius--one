package com.zenius.one.modules.itsecurity;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import com.zenius.one.platform.common.TenantContext;
import com.zenius.one.platform.config.DemoDatasetSeeder;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
@Tag(name = "IT Security & Governance", description = "IT asset inventory, security incident response, and organization security policies")
public class ItSecurityController {

    private final ItSecurityService securityService;
    private final DemoDatasetSeeder demoDatasetSeeder;

    public ItSecurityController(ItSecurityService securityService, DemoDatasetSeeder demoDatasetSeeder) {
        this.securityService = securityService;
        this.demoDatasetSeeder = demoDatasetSeeder;
    }

    @PostMapping("/seed-demo")
    @PreAuthorize("@authz.hasPermission('itsec:write')")
    public ApiResponse<DemoDatasetSeeder.SeedResult> seedDemoDataset(
            @AuthenticationPrincipal ZeniusUserPrincipal principal) {
        return ApiResponse.ok(
                demoDatasetSeeder.seedOrganization(TenantContext.requireOrganizationId(), principal.getUserId()));
    }

    @GetMapping("/assets")
    @PreAuthorize("@authz.hasPermission('itsec:read')")
    public ApiResponse<List<ItSecurityService.AssetSummary>> listAssets() {
        return ApiResponse.ok(securityService.listAssets());
    }

    @PostMapping("/assets")
    @PreAuthorize("@authz.hasPermission('itsec:write')")
    public ApiResponse<ItSecurityService.AssetSummary> createAsset(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody ItSecurityService.CreateAssetRequest request) {
        return ApiResponse.ok(securityService.createAsset(principal, request));
    }

    @GetMapping("/incidents")
    @PreAuthorize("@authz.hasPermission('itsec:read')")
    public ApiResponse<List<ItSecurityService.IncidentSummary>> listIncidents() {
        return ApiResponse.ok(securityService.listIncidents());
    }

    @PostMapping("/incidents")
    @PreAuthorize("@authz.hasPermission('itsec:write')")
    public ApiResponse<ItSecurityService.IncidentSummary> createIncident(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody ItSecurityService.CreateIncidentRequest request) {
        return ApiResponse.ok(securityService.createIncident(principal, request));
    }

    @PostMapping("/incidents/{id}/resolve")
    @PreAuthorize("@authz.hasPermission('itsec:write')")
    public ApiResponse<ItSecurityService.IncidentSummary> resolveIncident(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ItSecurityService.ResolveIncidentRequest request) {
        return ApiResponse.ok(securityService.resolveIncident(principal, id, request));
    }

    @GetMapping("/policy")
    @PreAuthorize("@authz.hasPermission('itsec:read')")
    public ApiResponse<ItSecurityService.PolicySummary> getPolicy() {
        return ApiResponse.ok(securityService.getPolicy());
    }

    @PutMapping("/policy")
    @PreAuthorize("@authz.hasPermission('itsec:write')")
    public ApiResponse<ItSecurityService.PolicySummary> updatePolicy(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody ItSecurityService.UpdatePolicyRequest request) {
        return ApiResponse.ok(securityService.updatePolicy(principal, request));
    }
}

