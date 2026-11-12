package com.zenius.one.modules.projects;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "proj_time_logs")
public class ProjTimeLogEntity extends TenantScopedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "member_name", nullable = false, length = 150)
    private String memberName;

    @Column(name = "minutes_logged", nullable = false)
    private Integer minutesLogged;

    @Column(name = "billable", nullable = false)
    private boolean billable = true;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "notes", length = 300)
    private String notes;

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public Integer getMinutesLogged() {
        return minutesLogged;
    }

    public void setMinutesLogged(Integer minutesLogged) {
        this.minutesLogged = minutesLogged;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

