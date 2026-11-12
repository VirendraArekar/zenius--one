package com.zenius.one.modules.hr;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_employees")
public class HrEmployeeEntity extends TenantScopedEntity {

    @Column(name = "employee_code", nullable = false, length = 40)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "work_email", nullable = false, length = 180)
    private String workEmail;

    @Column(name = "designation", nullable = false, length = 120)
    private String designation;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "manager_employee_id")
    private UUID managerEmployeeId;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "employment_type", nullable = false, length = 32)
    private String employmentType = "FULL_TIME";

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "base_monthly_salary", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseMonthlySalary = BigDecimal.ZERO;

    @Column(name = "annual_leave_balance", nullable = false)
    private Integer annualLeaveBalance = 18;

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getWorkEmail() {
        return workEmail;
    }

    public void setWorkEmail(String workEmail) {
        this.workEmail = workEmail;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public UUID getManagerEmployeeId() {
        return managerEmployeeId;
    }

    public void setManagerEmployeeId(UUID managerEmployeeId) {
        this.managerEmployeeId = managerEmployeeId;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getBaseMonthlySalary() {
        return baseMonthlySalary;
    }

    public void setBaseMonthlySalary(BigDecimal baseMonthlySalary) {
        this.baseMonthlySalary = baseMonthlySalary;
    }

    public Integer getAnnualLeaveBalance() {
        return annualLeaveBalance;
    }

    public void setAnnualLeaveBalance(Integer annualLeaveBalance) {
        this.annualLeaveBalance = annualLeaveBalance;
    }
}

