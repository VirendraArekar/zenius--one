package com.zenius.one.modules.identity.security;

import com.zenius.one.platform.common.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authz")
public class PermissionGuard {

    public boolean hasPermission(String permissionKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (!(auth.getPrincipal() instanceof ZeniusUserPrincipal principal)) {
            return false;
        }
        if (TenantContext.getOrganizationId().isEmpty() || principal.getActiveOrganizationId() == null) {
            return false;
        }
        return principal.getPermissions().contains(permissionKey);
    }
}

