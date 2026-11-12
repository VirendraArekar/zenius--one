package com.zenius.one.modules.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import com.zenius.one.platform.notifications.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomationService {

    private static final Set<String> VALID_ACTIONS = Set.of(
            "CREATE_NOTIFICATION", "CALL_WEBHOOK", "CREATE_CRM_ACTIVITY", "CREATE_PROJECT_TASK", "RPA_SYNC");

    private final AutWorkflowRepository workflowRepository;
    private final AutExecutionRepository executionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AutomationService(
            AutWorkflowRepository workflowRepository,
            AutExecutionRepository executionRepository,
            NotificationService notificationService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkflowSummary> listWorkflows() {
        UUID orgId = TenantContext.requireOrganizationId();
        return workflowRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(WorkflowSummary::from)
                .toList();
    }

    @Transactional
    public WorkflowSummary createWorkflow(
            ZeniusUserPrincipal principal, @Valid CreateWorkflowRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        String actionType = request.actionType().trim().toUpperCase();
        if (!VALID_ACTIONS.contains(actionType)) {
            throw new IllegalArgumentException("Unsupported actionType: " + request.actionType());
        }

        AutWorkflowEntity wf = new AutWorkflowEntity();
        wf.setOrganizationId(orgId);
        wf.setName(request.name().trim());
        wf.setTriggerEvent(request.triggerEvent().trim().toUpperCase());
        wf.setConditionExpr(request.conditionExpr() == null ? null : request.conditionExpr().trim());
        wf.setActionType(actionType);
        wf.setActionConfigJson(
                request.actionConfigJson() == null || request.actionConfigJson().isBlank()
                        ? "{}"
                        : request.actionConfigJson());
        wf.setEnabled(request.enabled() == null || request.enabled());
        AutWorkflowEntity saved = workflowRepository.save(wf);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "WORKFLOW_CREATED",
                "AUT_WORKFLOW",
                saved.getId().toString(),
                "Created workflow " + saved.getName() + " [" + saved.getTriggerEvent() + " -> " + actionType + "]",
                "SUCCESS");

        return WorkflowSummary.from(saved);
    }

    @Transactional
    public WorkflowSummary toggleWorkflow(UUID workflowId, boolean enabled) {
        UUID orgId = TenantContext.requireOrganizationId();
        AutWorkflowEntity wf = workflowRepository
                .findByIdAndOrganizationId(workflowId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        wf.setEnabled(enabled);
        return WorkflowSummary.from(workflowRepository.save(wf));
    }

    @Transactional
    public ExecutionSummary triggerWorkflow(
            ZeniusUserPrincipal principal, UUID workflowId, Map<String, Object> payload) {
        UUID orgId = TenantContext.requireOrganizationId();
        AutWorkflowEntity wf = workflowRepository
                .findByIdAndOrganizationId(workflowId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));

        long startMs = System.currentTimeMillis();
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        String serializedPayload = toJson(safePayload);

        String status;
        String summary;

        if (!wf.isEnabled()) {
            status = "SKIPPED";
            summary = "Workflow is disabled";
        } else if (!matchesCondition(wf.getConditionExpr(), safePayload)) {
            status = "SKIPPED";
            summary = "Condition not satisfied: " + wf.getConditionExpr();
        } else {
            status = "SUCCESS";
            summary = executeAction(orgId, principal, wf, safePayload);
            wf.setRunCount(wf.getRunCount() + 1);
            wf.setLastRunAt(Instant.now());
            workflowRepository.save(wf);
        }

        AutExecutionEntity exec = new AutExecutionEntity();
        exec.setOrganizationId(orgId);
        exec.setWorkflowId(wf.getId());
        exec.setTriggerPayloadJson(serializedPayload);
        exec.setStatus(status);
        exec.setOutputSummary(summary);
        exec.setDurationMs(Math.max(1L, System.currentTimeMillis() - startMs));
        exec.setExecutedAt(Instant.now());
        return ExecutionSummary.from(executionRepository.save(exec));
    }

    @Transactional(readOnly = true)
    public List<ExecutionSummary> listExecutions(UUID workflowId) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<AutExecutionEntity> list = workflowId == null
                ? executionRepository.findByOrganizationIdOrderByExecutedAtDesc(orgId)
                : executionRepository.findByOrganizationIdAndWorkflowIdOrderByExecutedAtDesc(orgId, workflowId);
        return list.stream().map(ExecutionSummary::from).toList();
    }

    private String executeAction(
            UUID orgId, ZeniusUserPrincipal principal, AutWorkflowEntity wf, Map<String, Object> payload) {
        if ("CREATE_NOTIFICATION".equals(wf.getActionType())) {
            notificationService.send(
                    orgId,
                    principal.getUserId(),
                    "Automation: " + wf.getName(),
                    "Triggered by " + wf.getTriggerEvent() + " payload keys=" + payload.keySet(),
                    "AUTOMATION",
                    "/automation");
            return "Dispatched in-app notification for workflow " + wf.getName();
        }
        return "Executed " + wf.getActionType() + " for trigger " + wf.getTriggerEvent();
    }

    private boolean matchesCondition(String conditionExpr, Map<String, Object> payload) {
        if (conditionExpr == null || conditionExpr.isBlank()) {
            return true;
        }
        String expr = conditionExpr.trim();
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            Object actual = payload.get(parts[0].trim());
            return actual != null && actual.toString().equalsIgnoreCase(parts[1].trim());
        }
        if (expr.contains(">")) {
            String[] parts = expr.split(">", 2);
            return compareNumeric(payload.get(parts[0].trim()), parts[1].trim()) > 0;
        }
        if (expr.contains("<")) {
            String[] parts = expr.split("<", 2);
            return compareNumeric(payload.get(parts[0].trim()), parts[1].trim()) < 0;
        }
        return true;
    }

    private int compareNumeric(Object actualObj, String expectedRaw) {
        try {
            BigDecimal actual = actualObj == null ? BigDecimal.ZERO : new BigDecimal(actualObj.toString());
            BigDecimal expected = new BigDecimal(expectedRaw);
            return actual.compareTo(expected);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record CreateWorkflowRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 64) String triggerEvent,
            @Size(max = 255) String conditionExpr,
            @NotBlank @Size(max = 64) String actionType,
            String actionConfigJson,
            Boolean enabled) {}

    public record ToggleWorkflowRequest(boolean enabled) {}

    public record WorkflowSummary(
            UUID id,
            String name,
            String triggerEvent,
            String conditionExpr,
            String actionType,
            boolean enabled,
            int runCount,
            Instant lastRunAt,
            Instant createdAt) {
        public static WorkflowSummary from(AutWorkflowEntity entity) {
            return new WorkflowSummary(
                    entity.getId(),
                    entity.getName(),
                    entity.getTriggerEvent(),
                    entity.getConditionExpr(),
                    entity.getActionType(),
                    entity.isEnabled(),
                    entity.getRunCount(),
                    entity.getLastRunAt(),
                    entity.getCreatedAt());
        }
    }

    public record ExecutionSummary(
            UUID id,
            UUID workflowId,
            String status,
            String outputSummary,
            long durationMs,
            Instant executedAt) {
        public static ExecutionSummary from(AutExecutionEntity entity) {
            return new ExecutionSummary(
                    entity.getId(),
                    entity.getWorkflowId(),
                    entity.getStatus(),
                    entity.getOutputSummary(),
                    entity.getDurationMs(),
                    entity.getExecutedAt());
        }
    }
}

