package com.zenius.one.modules.legal;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalController {

    private final LegalService legalService;

    public LegalController(LegalService legalService) {
        this.legalService = legalService;
    }

    @GetMapping("/contracts")
    @PreAuthorize("@authz.hasPermission('legal:read')")
    public ApiResponse<List<LegalService.ContractSummary>> listContracts() {
        return ApiResponse.ok(legalService.listContracts());
    }

    @PostMapping("/contracts")
    @PreAuthorize("@authz.hasPermission('legal:write')")
    public ApiResponse<LegalService.ContractSummary> createContract(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody LegalService.CreateContractRequest request) {
        return ApiResponse.ok(legalService.createContract(principal, request));
    }

    @PatchMapping("/contracts/{id}/status")
    @PreAuthorize("@authz.hasPermission('legal:write')")
    public ApiResponse<LegalService.ContractSummary> updateStatus(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody LegalService.UpdateStatusRequest request) {
        return ApiResponse.ok(legalService.updateContractStatus(principal, id, request));
    }
}

