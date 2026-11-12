package com.zenius.one.modules.organization;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.organization.MemberService.AddMemberRequest;
import com.zenius.one.modules.organization.MemberService.MemberDto;
import com.zenius.one.modules.organization.MemberService.UpdateMemberRequest;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v1/members")
@Validated
@Tag(name = "Employees & Members", description = "Organization employee directory, assignments, and role bindings")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('member:read')")
    @Operation(summary = "List and search members in the current organization")
    public ApiResponse<List<MemberDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<MemberDto> result = memberService.listMembers(
                q,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "joinedAt"))
        );
        return ApiResponse.page(result, result.getContent());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('member:write')")
    @Operation(summary = "Add or invite a user into the current organization")
    public ApiResponse<MemberDto> add(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody AddMemberBody body
    ) {
        return ApiResponse.ok(memberService.addOrInviteMember(
                principal,
                new AddMemberRequest(
                        body.fullName(),
                        body.email(),
                        body.temporaryPassword(),
                        body.employeeCode(),
                        body.designation(),
                        body.branchId(),
                        body.departmentId(),
                        body.roleIds()
                )
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.hasPermission('member:write')")
    @Operation(summary = "Update member department, branch, status, or assigned roles")
    public ApiResponse<MemberDto> update(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMemberBody body
    ) {
        return ApiResponse.ok(memberService.updateMember(
                principal,
                id,
                new UpdateMemberRequest(
                        body.employeeCode(),
                        body.designation(),
                        body.branchId(),
                        body.departmentId(),
                        body.status(),
                        body.roleIds()
                )
        ));
    }

    public record AddMemberBody(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String temporaryPassword,
            String employeeCode,
            String designation,
            UUID branchId,
            UUID departmentId,
            List<UUID> roleIds
    ) {
    }

    public record UpdateMemberBody(
            String employeeCode,
            String designation,
            UUID branchId,
            UUID departmentId,
            String status,
            List<UUID> roleIds
    ) {
    }
}

