package com.zenius.one.modules.crm;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_leads")
public class CrmLeadEntity extends TenantScopedEntity {

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "company_name", length = 180)
    private String companyName;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "source", nullable = false, length = 64)
    private String source = "WEBSITE";

    @Column(name = "status", nullable = false, length = 40)
    private String status = "NEW";

    @Column(name = "lead_score", nullable = false)
    private Integer leadScore = 20;

    @Column(name = "estimated_value", precision = 18, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "converted_account_id")
    private UUID convertedAccountId;

    @Column(name = "converted_contact_id")
    private UUID convertedContactId;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getConvertedAccountId() {
        return convertedAccountId;
    }

    public void setConvertedAccountId(UUID convertedAccountId) {
        this.convertedAccountId = convertedAccountId;
    }

    public UUID getConvertedContactId() {
        return convertedContactId;
    }

    public void setConvertedContactId(UUID convertedContactId) {
        this.convertedContactId = convertedContactId;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}

