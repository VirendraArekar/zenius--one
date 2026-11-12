package com.zenius.one.modules.organization;

import com.zenius.one.platform.common.BaseAuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class OrganizationEntity extends BaseAuditedEntity {

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency = "USD";

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

