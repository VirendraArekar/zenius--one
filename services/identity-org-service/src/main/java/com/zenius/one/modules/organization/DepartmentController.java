package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.organization.DepartmentService.DepartmentDto;
import com.zenius.one.modules.organization.DepartmentService.DepartmentMutationRequest;
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
@RequestMapping("/api/v1/departments")
@Validated
@Tag(name = "Departments", description = "Organization department management")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('department:read')")
    @Operation(summary = "List active departments for the current organization")
    public ApiResponse<List<DepartmentDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<DepartmentDto> result = departmentService.list(q, PageRequest.of(page, size, Sort.by("name").ascending()));
        return ApiResponse.page(result, result.getContent());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('department:write')")
    @Operation(summary = "Create a new department")
    public ApiResponse<DepartmentDto> create(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody DepartmentBody body
    ) {
        return ApiResponse.ok(departmentService.create(
                principal,
                new DepartmentMutationRequest(body.code(), body.name(), body.description(), body.parentDepartmentId())
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.hasPermission('department:write')")
    @Operation(summary = "Update a department")
    public ApiResponse<DepartmentDto> update(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentBody body
    ) {
        return ApiResponse.ok(departmentService.update(
                principal,
                id,
                new DepartmentMutationRequest(body.code(), body.name(), body.description(), body.parentDepartmentId())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.hasPermission('department:write')")
    @Operation(summary = "Archive a department")
    public ApiResponse<Map<String, Boolean>> archive(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        departmentService.archive(principal, id);
        return ApiResponse.ok(Map.of("archived", true));
    }

    public record DepartmentBody(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            String description,
            UUID parentDepartmentId
    ) {
    }
}

