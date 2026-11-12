package com.zenius.one.modules.marketing;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mkt_subscribers")
public class MktSubscriberEntity extends TenantScopedEntity {

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "full_name", length = 160)
    private String fullName;

    @Column(name = "source", nullable = false, length = 64)
    private String source = "MANUAL";

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "SUBSCRIBED";

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

