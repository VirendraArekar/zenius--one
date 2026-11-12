package com.zenius.one.modules.automation;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "aut_workflows")
public class AutWorkflowEntity extends TenantScopedEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "trigger_event", nullable = false, length = 64)
    private String triggerEvent;

    @Column(name = "condition_expr", length = 255)
    private String conditionExpr;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "action_config_json", nullable = false, columnDefinition = "TEXT")
    private String actionConfigJson = "{}";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "run_count", nullable = false)
    private int runCount = 0;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public String getConditionExpr() {
        return conditionExpr;
    }

    public void setConditionExpr(String conditionExpr) {
        this.conditionExpr = conditionExpr;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionConfigJson() {
        return actionConfigJson;
    }

    public void setActionConfigJson(String actionConfigJson) {
        this.actionConfigJson = actionConfigJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }
}

