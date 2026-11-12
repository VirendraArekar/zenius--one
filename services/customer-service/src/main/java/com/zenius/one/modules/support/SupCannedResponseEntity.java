package com.zenius.one.modules.support;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sup_canned_responses")
public class SupCannedResponseEntity extends TenantScopedEntity {

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}

