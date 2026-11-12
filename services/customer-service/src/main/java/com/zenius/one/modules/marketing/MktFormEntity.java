package com.zenius.one.modules.marketing;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mkt_forms")
public class MktFormEntity extends TenantScopedEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "submit_label", nullable = false, length = 64)
    private String submitLabel = "Submit";

    @Column(name = "auto_create_lead", nullable = false)
    private boolean autoCreateLead = true;

    @Column(name = "submissions_count", nullable = false)
    private Integer submissionsCount = 0;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubmitLabel() {
        return submitLabel;
    }

    public void setSubmitLabel(String submitLabel) {
        this.submitLabel = submitLabel;
    }

    public boolean isAutoCreateLead() {
        return autoCreateLead;
    }

    public void setAutoCreateLead(boolean autoCreateLead) {
        this.autoCreateLead = autoCreateLead;
    }

    public Integer getSubmissionsCount() {
        return submissionsCount;
    }

    public void setSubmissionsCount(Integer submissionsCount) {
        this.submissionsCount = submissionsCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

