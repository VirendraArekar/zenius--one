package com.zenius.one.modules.crm;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "crm_stages")
public class CrmStageEntity extends TenantScopedEntity {

    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "probability", nullable = false)
    private Integer probability = 20;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    @Column(name = "is_won", nullable = false)
    private boolean won;

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getStageOrder() {
        return stageOrder;
    }

    public void setStageOrder(Integer stageOrder) {
        this.stageOrder = stageOrder;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }
}

