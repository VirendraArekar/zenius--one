package com.zenius.one.platform.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local multi-tenant organization context populated by security filters (Phase 2+).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_ORG_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrganizationId(UUID organizationId) {
        CURRENT_ORG_ID.set(organizationId);
    }

    public static Optional<UUID> getOrganizationId() {
        return Optional.ofNullable(CURRENT_ORG_ID.get());
    }

    public static UUID requireOrganizationId() {
        return getOrganizationId()
                .orElseThrow(() -> new IllegalStateException("Active organization context is required"));
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
    }
}

