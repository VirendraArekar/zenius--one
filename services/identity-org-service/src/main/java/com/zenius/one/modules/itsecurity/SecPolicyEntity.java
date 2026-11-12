package com.zenius.one.modules.itsecurity;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sec_policies")
public class SecPolicyEntity extends TenantScopedEntity {

    @Column(name = "min_password_length", nullable = false)
    private int minPasswordLength = 12;

    @Column(name = "require_mfa", nullable = false)
    private boolean requireMfa;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes = 480;

    @Column(name = "allowed_ip_cidr", length = 120)
    private String allowedIpCidr = "0.0.0.0/0";

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public void setMinPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }

    public boolean isRequireMfa() {
        return requireMfa;
    }

    public void setRequireMfa(boolean requireMfa) {
        this.requireMfa = requireMfa;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public String getAllowedIpCidr() {
        return allowedIpCidr;
    }

    public void setAllowedIpCidr(String allowedIpCidr) {
        this.allowedIpCidr = allowedIpCidr;
    }
}

