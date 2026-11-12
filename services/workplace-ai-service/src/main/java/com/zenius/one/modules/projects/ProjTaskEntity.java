package com.zenius.one.modules.projects;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "proj_tasks")
public class ProjTaskEntity extends TenantScopedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "task_key", nullable = false, length = 48)
    private String taskKey;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "task_type", nullable = false, length = 24)
    private String taskType = "TASK";

    @Column(name = "priority", nullable = false, length = 24)
    private String priority = "MEDIUM";

    @Column(name = "status", nullable = false, length = 32)
    private String status = "TODO";

    @Column(name = "story_points", nullable = false)
    private Integer storyPoints = 1;

    @Column(name = "assignee_name", length = 150)
    private String assigneeName;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "logged_minutes", nullable = false)
    private Integer loggedMinutes = 0;

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getSprintId() {
        return sprintId;
    }

    public void setSprintId(UUID sprintId) {
        this.sprintId = sprintId;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Integer storyPoints) {
        this.storyPoints = storyPoints;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getLoggedMinutes() {
        return loggedMinutes;
    }

    public void setLoggedMinutes(Integer loggedMinutes) {
        this.loggedMinutes = loggedMinutes;
    }
}

