package com.zenius.one.modules.creator;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/creator")
@Tag(name = "Low-Code Creator", description = "Custom low-code applications, dynamic forms, formulas, and records")
public class CreatorController {

    private final CreatorService creatorService;

    public CreatorController(CreatorService creatorService) {
        this.creatorService = creatorService;
    }

    @GetMapping("/apps")
    @PreAuthorize("@authz.hasPermission('creator:read')")
    public ApiResponse<List<CreatorService.AppSummary>> listApps() {
        return ApiResponse.ok(creatorService.listApps());
    }

    @PostMapping("/apps")
    @PreAuthorize("@authz.hasPermission('creator:write')")
    public ApiResponse<CreatorService.AppSummary> createApp(
            @Valid @RequestBody CreatorService.CreateAppRequest request) {
        return ApiResponse.ok(creatorService.createApp(request));
    }

    @PostMapping("/apps/{id}/publish")
    @PreAuthorize("@authz.hasPermission('creator:write')")
    public ApiResponse<CreatorService.AppSummary> publishApp(
            @AuthenticationPrincipal ZeniusUserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(creatorService.publishApp(principal, id));
    }

    @GetMapping("/forms")
    @PreAuthorize("@authz.hasPermission('creator:read')")
    public ApiResponse<List<CreatorService.FormSummary>> listForms(@RequestParam(required = false) UUID appId) {
        return ApiResponse.ok(creatorService.listForms(appId));
    }

    @PostMapping("/forms")
    @PreAuthorize("@authz.hasPermission('creator:write')")
    public ApiResponse<CreatorService.FormSummary> createForm(
            @Valid @RequestBody CreatorService.CreateFormRequest request) {
        return ApiResponse.ok(creatorService.createForm(request));
    }

    @GetMapping("/forms/{formId}/records")
    @PreAuthorize("@authz.hasPermission('creator:read')")
    public ApiResponse<List<CreatorService.RecordSummary>> listRecords(@PathVariable UUID formId) {
        return ApiResponse.ok(creatorService.listRecords(formId));
    }

    @PostMapping("/forms/{formId}/records")
    @PreAuthorize("@authz.hasPermission('creator:write')")
    public ApiResponse<CreatorService.RecordSummary> submitRecord(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID formId,
            @Valid @RequestBody CreatorService.SubmitRecordRequest request) {
        return ApiResponse.ok(creatorService.submitRecord(principal, formId, request));
    }
}

