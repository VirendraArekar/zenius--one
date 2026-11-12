package com.zenius.one.modules.crm;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_accounts")
public class CrmAccountEntity extends TenantScopedEntity {

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "annual_revenue", precision = 18, scale = 2)
    private BigDecimal annualRevenue;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "lifecycle_stage", nullable = false, length = 40)
    private String lifecycleStage = "PROSPECT";

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_country", length = 100)
    private String billingCountry;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getParentAccountId() {
        return parentAccountId;
    }

    public void setParentAccountId(UUID parentAccountId) {
        this.parentAccountId = parentAccountId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getAnnualRevenue() {
        return annualRevenue;
    }

    public void setAnnualRevenue(BigDecimal annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public String getLifecycleStage() {
        return lifecycleStage;
    }

    public void setLifecycleStage(String lifecycleStage) {
        this.lifecycleStage = lifecycleStage;
    }

    public String getBillingCity() {
        return billingCity;
    }

    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }

    public String getBillingCountry() {
        return billingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}

