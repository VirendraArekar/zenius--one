package com.zenius.one.modules.itsecurity;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sec_it_assets")
public class SecItAssetEntity extends TenantScopedEntity {

    @Column(name = "asset_tag", nullable = false, length = 40)
    private String assetTag;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "asset_type", nullable = false, length = 40)
    private String assetType;

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(name = "os_version", length = 100)
    private String osVersion;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "compliance_state", nullable = false, length = 32)
    private String complianceState = "COMPLIANT";

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComplianceState() {
        return complianceState;
    }

    public void setComplianceState(String complianceState) {
        this.complianceState = complianceState;
    }
}

