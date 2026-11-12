package com.zenius.one.modules.organization;

import com.zenius.one.platform.common.BaseAuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class PermissionEntity extends BaseAuditedEntity {

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "permission_key", nullable = false, unique = true, length = 120)
    private String permissionKey;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public void setPermissionKey(String permissionKey) {
        this.permissionKey = permissionKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

