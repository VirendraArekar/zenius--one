package com.zenius.one.modules.marketing;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mkt_journeys")
public class MktJourneyEntity extends TenantScopedEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "trigger_event", nullable = false, length = 64)
    private String triggerEvent;

    @Column(name = "steps_summary", nullable = false, length = 500)
    private String stepsSummary;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "enrolled_count", nullable = false)
    private Integer enrolledCount = 0;

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

    public String getStepsSummary() {
        return stepsSummary;
    }

    public void setStepsSummary(String stepsSummary) {
        this.stepsSummary = stepsSummary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(Integer enrolledCount) {
        this.enrolledCount = enrolledCount;
    }
}

