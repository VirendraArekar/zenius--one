package com.zenius.one.modules.creator;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "crt_forms")
public class CrtFormEntity extends TenantScopedEntity {

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "form_code", nullable = false, length = 40)
    private String formCode;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "schema_json", nullable = false, columnDefinition = "TEXT")
    private String schemaJson = "[]";

    public UUID getAppId() {
        return appId;
    }

    public void setAppId(UUID appId) {
        this.appId = appId;
    }

    public String getFormCode() {
        return formCode;
    }

    public void setFormCode(String formCode) {
        this.formCode = formCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }
}

