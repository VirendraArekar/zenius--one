package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.organization.BranchService.BranchDto;
import com.zenius.one.modules.organization.BranchService.BranchMutationRequest;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branches")
@Validated
@Tag(name = "Branches", description = "Organization branch and location management")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('branch:read')")
    @Operation(summary = "List active branches for the current organization")
    public ApiResponse<List<BranchDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<BranchDto> result = branchService.list(q, PageRequest.of(page, size, Sort.by("name").ascending()));
        return ApiResponse.page(result, result.getContent());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('branch:write')")
    @Operation(summary = "Create a new branch")
    public ApiResponse<BranchDto> create(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody BranchBody body
    ) {
        return ApiResponse.ok(branchService.create(
                principal,
                new BranchMutationRequest(body.code(), body.name(), body.city(), body.country(), body.address(), body.headquarters())
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.hasPermission('branch:write')")
    @Operation(summary = "Update an existing branch")
    public ApiResponse<BranchDto> update(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody BranchBody body
    ) {
        return ApiResponse.ok(branchService.update(
                principal,
                id,
                new BranchMutationRequest(body.code(), body.name(), body.city(), body.country(), body.address(), body.headquarters())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.hasPermission('branch:write')")
    @Operation(summary = "Archive a branch")
    public ApiResponse<Map<String, Boolean>> archive(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        branchService.archive(principal, id);
        return ApiResponse.ok(Map.of("archived", true));
    }

    public record BranchBody(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            String city,
            String country,
            String address,
            boolean headquarters
    ) {
    }
}

