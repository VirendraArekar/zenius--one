package com.zenius.one.modules.integration;

import com.zenius.one.modules.identity.security.TokenHashing;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditLogService auditLogService;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AuditLogService auditLogService) {
        this.apiKeyRepository = apiKeyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDto> listKeys() {
        UUID orgId = TenantContext.requireOrganizationId();
        return apiKeyRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CreatedApiKeyResponse createKey(ZeniusUserPrincipal principal, CreateApiKeyRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String rawSecret = "z1_live_" + TokenHashing.generateOpaqueToken(28);
        String prefix = rawSecret.substring(0, 14);
        String hash = TokenHashing.sha256Hex(rawSecret);

        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setOrganizationId(orgId);
        entity.setCreatedByUserId(principal.getUserId());
        entity.setName(req.name().trim());
        entity.setKeyPrefix(prefix);
        entity.setKeyHash(hash);
        entity.setScopes(String.join(",", req.scopes()));
        if (req.expiresInDays() != null && req.expiresInDays() > 0) {
            entity.setExpiresAt(Instant.now().plus(req.expiresInDays(), ChronoUnit.DAYS));
        }
        entity = apiKeyRepository.save(entity);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "API_KEY_CREATED",
                "API_KEY",
                entity.getId().toString(),
                "Created API key " + entity.getName() + " (" + prefix + "...)",
                "SUCCESS"
        );
        return new CreatedApiKeyResponse(toDto(entity), rawSecret);
    }

    @Transactional
    public void revokeKey(ZeniusUserPrincipal principal, UUID keyId) {
        UUID orgId = TenantContext.requireOrganizationId();
        ApiKeyEntity entity = apiKeyRepository.findByIdAndOrganizationId(keyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + keyId));
        entity.setRevokedAt(Instant.now());
        apiKeyRepository.save(entity);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "API_KEY_REVOKED",
                "API_KEY",
                entity.getId().toString(),
                "Revoked API key " + entity.getName(),
                "SUCCESS"
        );
    }

    private ApiKeyDto toDto(ApiKeyEntity e) {
        return new ApiKeyDto(
                e.getId(),
                e.getName(),
                e.getKeyPrefix(),
                e.getScopes(),
                e.getExpiresAt(),
                e.getLastUsedAt(),
                e.getRevokedAt(),
                e.getCreatedAt(),
                e.isActive(Instant.now())
        );
    }

    public record CreateApiKeyRequest(String name, List<String> scopes, Integer expiresInDays) {
    }

    public record ApiKeyDto(
            UUID id,
            String name,
            String keyPrefix,
            String scopes,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant revokedAt,
            Instant createdAt,
            boolean active
    ) {
    }

    public record CreatedApiKeyResponse(ApiKeyDto apiKey, String secretKey) {
    }
}

