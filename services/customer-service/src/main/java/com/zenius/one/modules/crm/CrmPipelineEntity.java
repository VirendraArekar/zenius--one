package com.zenius.one.modules.crm;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_pipelines")
public class CrmPipelineEntity extends TenantScopedEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPipeline;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefaultPipeline() {
        return defaultPipeline;
    }

    public void setDefaultPipeline(boolean defaultPipeline) {
        this.defaultPipeline = defaultPipeline;
    }
}

