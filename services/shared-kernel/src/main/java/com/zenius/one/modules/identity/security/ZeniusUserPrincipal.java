package com.zenius.one.modules.identity.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class ZeniusUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final UUID activeOrganizationId;
    private final Set<String> permissions;

    public ZeniusUserPrincipal(
            UUID userId,
            String email,
            String fullName,
            UUID activeOrganizationId,
            Set<String> permissions
    ) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.activeOrganizationId = activeOrganizationId;
        this.permissions = permissions;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getActiveOrganizationId() {
        return activeOrganizationId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }
}

