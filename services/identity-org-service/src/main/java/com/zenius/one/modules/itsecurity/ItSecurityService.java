package com.zenius.one.modules.itsecurity;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItSecurityService {

    private final SecItAssetRepository assetRepository;
    private final SecIncidentRepository incidentRepository;
    private final SecPolicyRepository policyRepository;
    private final AuditLogService auditLogService;

    public ItSecurityService(
            SecItAssetRepository assetRepository,
            SecIncidentRepository incidentRepository,
            SecPolicyRepository policyRepository,
            AuditLogService auditLogService) {
        this.assetRepository = assetRepository;
        this.incidentRepository = incidentRepository;
        this.policyRepository = policyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AssetSummary> listAssets() {
        UUID orgId = TenantContext.requireOrganizationId();
        return assetRepository.findByOrganizationIdOrderByAssetTagAsc(orgId).stream()
                .map(AssetSummary::from)
                .toList();
    }

    @Transactional
    public AssetSummary createAsset(ZeniusUserPrincipal principal, @Valid CreateAssetRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        long seq = assetRepository.countByOrganizationId(orgId) + 1;
        SecItAssetEntity asset = new SecItAssetEntity();
        asset.setOrganizationId(orgId);
        asset.setAssetTag(String.format("AST-%04d", seq));
        asset.setName(request.name().trim());
        asset.setAssetType(request.assetType().trim().toUpperCase());
        asset.setOwnerName(request.ownerName() == null ? null : request.ownerName().trim());
        asset.setOsVersion(request.osVersion() == null ? null : request.osVersion().trim());
        asset.setStatus("ACTIVE");
        asset.setComplianceState("COMPLIANT");
        SecItAssetEntity saved = assetRepository.save(asset);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "IT_ASSET_REGISTERED",
                "SEC_IT_ASSET",
                saved.getId().toString(),
                "Registered IT asset " + saved.getAssetTag() + " (" + saved.getName() + ")",
                "SUCCESS");

        return AssetSummary.from(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentSummary> listIncidents() {
        UUID orgId = TenantContext.requireOrganizationId();
        return incidentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(IncidentSummary::from)
                .toList();
    }

    @Transactional
    public IncidentSummary createIncident(ZeniusUserPrincipal principal, @Valid CreateIncidentRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        long seq = incidentRepository.countByOrganizationId(orgId) + 1;
        SecIncidentEntity incident = new SecIncidentEntity();
        incident.setOrganizationId(orgId);
        incident.setIncidentNumber(String.format("INC-%04d", seq));
        incident.setTitle(request.title().trim());
        incident.setSeverity(
                request.severity() == null || request.severity().isBlank()
                        ? "MEDIUM"
                        : request.severity().trim().toUpperCase());
        incident.setStatus("OPEN");
        incident.setReporterEmail(principal.getEmail());
        SecIncidentEntity saved = incidentRepository.save(incident);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "SECURITY_INCIDENT_OPENED",
                "SEC_INCIDENT",
                saved.getId().toString(),
                "Opened security incident " + saved.getIncidentNumber() + " [" + saved.getSeverity() + "]",
                "SUCCESS");

        return IncidentSummary.from(saved);
    }

    @Transactional
    public IncidentSummary resolveIncident(
            ZeniusUserPrincipal principal, UUID incidentId, @Valid ResolveIncidentRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        SecIncidentEntity incident = incidentRepository
                .findByIdAndOrganizationId(incidentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Security incident not found"));
        incident.setStatus("RESOLVED");
        incident.setResolutionNotes(request.resolutionNotes().trim());
        incident.setResolvedAt(Instant.now());
        SecIncidentEntity saved = incidentRepository.save(incident);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "SECURITY_INCIDENT_RESOLVED",
                "SEC_INCIDENT",
                saved.getId().toString(),
                "Resolved security incident " + saved.getIncidentNumber(),
                "SUCCESS");

        return IncidentSummary.from(saved);
    }

    @Transactional
    public PolicySummary getPolicy() {
        UUID orgId = TenantContext.requireOrganizationId();
        SecPolicyEntity policy = policyRepository.findByOrganizationId(orgId).orElseGet(() -> {
            SecPolicyEntity def = new SecPolicyEntity();
            def.setOrganizationId(orgId);
            return policyRepository.save(def);
        });
        return PolicySummary.from(policy);
    }

    @Transactional
    public PolicySummary updatePolicy(ZeniusUserPrincipal principal, @Valid UpdatePolicyRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        SecPolicyEntity policy = policyRepository.findByOrganizationId(orgId).orElseGet(() -> {
            SecPolicyEntity def = new SecPolicyEntity();
            def.setOrganizationId(orgId);
            return def;
        });
        policy.setMinPasswordLength(request.minPasswordLength());
        policy.setRequireMfa(request.requireMfa());
        policy.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
        policy.setAllowedIpCidr(
                request.allowedIpCidr() == null || request.allowedIpCidr().isBlank()
                        ? "0.0.0.0/0"
                        : request.allowedIpCidr().trim());
        SecPolicyEntity saved = policyRepository.save(policy);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "SECURITY_POLICY_UPDATED",
                "SEC_POLICY",
                saved.getId().toString(),
                "Updated org security policy (requireMfa=" + saved.isRequireMfa() + ", minPwd=" + saved.getMinPasswordLength() + ")",
                "SUCCESS");

        return PolicySummary.from(saved);
    }

    public record CreateAssetRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 40) String assetType,
            @Size(max = 150) String ownerName,
            @Size(max = 100) String osVersion) {}

    public record CreateIncidentRequest(@NotBlank @Size(max = 180) String title, String severity) {}

    public record ResolveIncidentRequest(@NotBlank @Size(max = 600) String resolutionNotes) {}

    public record UpdatePolicyRequest(
            @Min(8) @Max(64) int minPasswordLength,
            boolean requireMfa,
            @Min(15) @Max(1440) int sessionTimeoutMinutes,
            @Size(max = 120) String allowedIpCidr) {}

    public record AssetSummary(
            UUID id,
            String assetTag,
            String name,
            String assetType,
            String ownerName,
            String osVersion,
            String status,
            String complianceState,
            Instant createdAt) {
        public static AssetSummary from(SecItAssetEntity e) {
            return new AssetSummary(
                    e.getId(),
                    e.getAssetTag(),
                    e.getName(),
                    e.getAssetType(),
                    e.getOwnerName(),
                    e.getOsVersion(),
                    e.getStatus(),
                    e.getComplianceState(),
                    e.getCreatedAt());
        }
    }

    public record IncidentSummary(
            UUID id,
            String incidentNumber,
            String title,
            String severity,
            String status,
            String reporterEmail,
            String resolutionNotes,
            Instant resolvedAt,
            Instant createdAt) {
        public static IncidentSummary from(SecIncidentEntity e) {
            return new IncidentSummary(
                    e.getId(),
                    e.getIncidentNumber(),
                    e.getTitle(),
                    e.getSeverity(),
                    e.getStatus(),
                    e.getReporterEmail(),
                    e.getResolutionNotes(),
                    e.getResolvedAt(),
                    e.getCreatedAt());
        }
    }

    public record PolicySummary(
            UUID id, int minPasswordLength, boolean requireMfa, int sessionTimeoutMinutes, String allowedIpCidr) {
        public static PolicySummary from(SecPolicyEntity e) {
            return new PolicySummary(
                    e.getId(),
                    e.getMinPasswordLength(),
                    e.isRequireMfa(),
                    e.getSessionTimeoutMinutes(),
                    e.getAllowedIpCidr());
        }
    }
}

