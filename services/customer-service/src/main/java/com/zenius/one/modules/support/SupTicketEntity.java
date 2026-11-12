package com.zenius.one.modules.support;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sup_tickets")
public class SupTicketEntity extends TenantScopedEntity {

    @Column(name = "ticket_number", nullable = false, length = 40)
    private String ticketNumber;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel = "WEB";

    @Column(name = "priority", nullable = false, length = 32)
    private String priority = "MEDIUM";

    @Column(name = "status", nullable = false, length = 32)
    private String status = "OPEN";

    @Column(name = "requester_name", nullable = false, length = 150)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 180)
    private String requesterEmail;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "escalated", nullable = false)
    private boolean escalated;

    @Column(name = "csat_rating")
    private Integer csatRating;

    @Column(name = "csat_feedback", length = 500)
    private String csatFeedback;

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(UUID assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public Instant getSlaDueAt() {
        return slaDueAt;
    }

    public void setSlaDueAt(Instant slaDueAt) {
        this.slaDueAt = slaDueAt;
    }

    public Instant getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public void setFirstRespondedAt(Instant firstRespondedAt) {
        this.firstRespondedAt = firstRespondedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public Integer getCsatRating() {
        return csatRating;
    }

    public void setCsatRating(Integer csatRating) {
        this.csatRating = csatRating;
    }

    public String getCsatFeedback() {
        return csatFeedback;
    }

    public void setCsatFeedback(String csatFeedback) {
        this.csatFeedback = csatFeedback;
    }
}

