package com.zenius.one.modules.integration;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.integration.ApiKeyService.ApiKeyDto;
import com.zenius.one.modules.integration.ApiKeyService.CreateApiKeyRequest;
import com.zenius.one.modules.integration.ApiKeyService.CreatedApiKeyResponse;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/api-keys")
@Tag(name = "API Keys", description = "Organization-scoped API key issuance and revocation")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('apikey:read')")
    @Operation(summary = "List API keys for the current organization")
    public ApiResponse<List<ApiKeyDto>> listKeys() {
        return ApiResponse.ok(apiKeyService.listKeys());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('apikey:write')")
    @Operation(summary = "Generate a new organization API key (plaintext returned once)")
    public ApiResponse<CreatedApiKeyResponse> createKey(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody CreateApiKeyBody body
    ) {
        return ApiResponse.ok(apiKeyService.createKey(
                principal,
                new CreateApiKeyRequest(body.name(), body.scopes(), body.expiresInDays())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.hasPermission('apikey:write')")
    @Operation(summary = "Revoke an organization API key")
    public ApiResponse<Map<String, Boolean>> revokeKey(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        apiKeyService.revokeKey(principal, id);
        return ApiResponse.ok(Map.of("revoked", true));
    }

    public record CreateApiKeyBody(
            @NotBlank String name,
            @NotEmpty List<String> scopes,
            Integer expiresInDays
    ) {
    }
}

