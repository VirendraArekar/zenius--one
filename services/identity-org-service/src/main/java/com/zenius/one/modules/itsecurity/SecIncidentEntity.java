package com.zenius.one.modules.itsecurity;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sec_incidents")
public class SecIncidentEntity extends TenantScopedEntity {

    @Column(name = "incident_number", nullable = false, length = 40)
    private String incidentNumber;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 24)
    private String severity = "MEDIUM";

    @Column(nullable = false, length = 24)
    private String status = "OPEN";

    @Column(name = "reporter_email", nullable = false, length = 180)
    private String reporterEmail;

    @Column(name = "resolution_notes", length = 600)
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

