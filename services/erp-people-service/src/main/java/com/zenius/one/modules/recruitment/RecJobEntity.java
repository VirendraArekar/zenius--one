package com.zenius.one.modules.recruitment;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "rec_jobs")
public class RecJobEntity extends TenantScopedEntity {

    @Column(name = "job_code", nullable = false, length = 40)
    private String jobCode;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "location", length = 120)
    private String location;

    @Column(name = "employment_type", nullable = false, length = 32)
    private String employmentType = "FULL_TIME";

    @Column(name = "openings_count", nullable = false)
    private Integer openingsCount = 1;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "OPEN";

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Integer getOpeningsCount() {
        return openingsCount;
    }

    public void setOpeningsCount(Integer openingsCount) {
        this.openingsCount = openingsCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

