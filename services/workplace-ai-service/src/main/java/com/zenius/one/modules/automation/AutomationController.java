package com.zenius.one.modules.automation;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/automation")
@Tag(name = "Workflow Automation & RPA", description = "Event-driven workflow rules, conditions, notifications, and RPA execution logs")
public class AutomationController {

    private final AutomationService automationService;

    public AutomationController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @GetMapping("/workflows")
    @PreAuthorize("@authz.hasPermission('automation:read')")
    public ApiResponse<List<AutomationService.WorkflowSummary>> listWorkflows() {
        return ApiResponse.ok(automationService.listWorkflows());
    }

    @PostMapping("/workflows")
    @PreAuthorize("@authz.hasPermission('automation:write')")
    public ApiResponse<AutomationService.WorkflowSummary> createWorkflow(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody AutomationService.CreateWorkflowRequest request) {
        return ApiResponse.ok(automationService.createWorkflow(principal, request));
    }

    @PatchMapping("/workflows/{id}/toggle")
    @PreAuthorize("@authz.hasPermission('automation:write')")
    public ApiResponse<AutomationService.WorkflowSummary> toggleWorkflow(
            @PathVariable UUID id, @RequestBody AutomationService.ToggleWorkflowRequest request) {
        return ApiResponse.ok(automationService.toggleWorkflow(id, request.enabled()));
    }

    @PostMapping("/workflows/{id}/trigger")
    @PreAuthorize("@authz.hasPermission('automation:write')")
    public ApiResponse<AutomationService.ExecutionSummary> triggerWorkflow(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.ok(automationService.triggerWorkflow(principal, id, payload));
    }

    @GetMapping("/executions")
    @PreAuthorize("@authz.hasPermission('automation:read')")
    public ApiResponse<List<AutomationService.ExecutionSummary>> listExecutions(
            @RequestParam(required = false) UUID workflowId) {
        return ApiResponse.ok(automationService.listExecutions(workflowId));
    }
}

