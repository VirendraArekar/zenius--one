package com.zenius.one.modules.automation;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aut_executions")
public class AutExecutionEntity extends TenantScopedEntity {

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "trigger_payload_json", nullable = false, columnDefinition = "TEXT")
    private String triggerPayloadJson = "{}";

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "output_summary", nullable = false, length = 500)
    private String outputSummary;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs = 0;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public String getTriggerPayloadJson() {
        return triggerPayloadJson;
    }

    public void setTriggerPayloadJson(String triggerPayloadJson) {
        this.triggerPayloadJson = triggerPayloadJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}

