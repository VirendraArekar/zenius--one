package com.zenius.one.modules.inventory;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "inv_warehouses")
public class InvWarehouseEntity extends TenantScopedEntity {

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 140)
    private String name;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "is_default", nullable = false)
    private boolean defaultWarehouse;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isDefaultWarehouse() {
        return defaultWarehouse;
    }

    public void setDefaultWarehouse(boolean defaultWarehouse) {
        this.defaultWarehouse = defaultWarehouse;
    }
}

