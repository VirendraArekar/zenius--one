package com.zenius.one.platform.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.UUID;

/**
 * Base entity for all organization-scoped records. Enforces active TenantContext alignment on writes.
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseAuditedEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @PrePersist
    protected void stampAndVerifyTenant() {
        var ctxOrg = TenantContext.getOrganizationId();
        if (organizationId == null) {
            organizationId = ctxOrg.orElseThrow(
                    () -> new IllegalStateException("Tenant organizationId is required for persistence")
            );
        } else if (ctxOrg.isPresent() && !ctxOrg.get().equals(organizationId)) {
            throw new IllegalStateException("Cross-tenant write attempt blocked");
        }
    }

    @PreUpdate
    protected void verifyTenantOnUpdate() {
        var ctxOrg = TenantContext.getOrganizationId();
        if (ctxOrg.isPresent() && !ctxOrg.get().equals(organizationId)) {
            throw new IllegalStateException("Cross-tenant update attempt blocked");
        }
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}

