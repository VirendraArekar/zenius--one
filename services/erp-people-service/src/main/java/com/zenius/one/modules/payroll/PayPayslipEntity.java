package com.zenius.one.modules.payroll;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pay_payslips")
public class PayPayslipEntity extends TenantScopedEntity {

    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employee_name", nullable = false, length = 160)
    private String employeeName;

    @Column(name = "pay_month", nullable = false, length = 16)
    private String payMonth;

    @Column(name = "basic_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal basicPay;

    @Column(name = "hra_allowance", nullable = false, precision = 18, scale = 2)
    private BigDecimal hraAllowance;

    @Column(name = "special_allowance", nullable = false, precision = 18, scale = 2)
    private BigDecimal specialAllowance;

    @Column(name = "gross_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "pf_deduction", nullable = false, precision = 18, scale = 2)
    private BigDecimal pfDeduction;

    @Column(name = "esi_deduction", nullable = false, precision = 18, scale = 2)
    private BigDecimal esiDeduction;

    @Column(name = "tds_tax_deduction", nullable = false, precision = 18, scale = 2)
    private BigDecimal tdsTaxDeduction;

    @Column(name = "total_deductions", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal netPay;

    public UUID getPayrollRunId() {
        return payrollRunId;
    }

    public void setPayrollRunId(UUID payrollRunId) {
        this.payrollRunId = payrollRunId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getPayMonth() {
        return payMonth;
    }

    public void setPayMonth(String payMonth) {
        this.payMonth = payMonth;
    }

    public BigDecimal getBasicPay() {
        return basicPay;
    }

    public void setBasicPay(BigDecimal basicPay) {
        this.basicPay = basicPay;
    }

    public BigDecimal getHraAllowance() {
        return hraAllowance;
    }

    public void setHraAllowance(BigDecimal hraAllowance) {
        this.hraAllowance = hraAllowance;
    }

    public BigDecimal getSpecialAllowance() {
        return specialAllowance;
    }

    public void setSpecialAllowance(BigDecimal specialAllowance) {
        this.specialAllowance = specialAllowance;
    }

    public BigDecimal getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(BigDecimal grossPay) {
        this.grossPay = grossPay;
    }

    public BigDecimal getPfDeduction() {
        return pfDeduction;
    }

    public void setPfDeduction(BigDecimal pfDeduction) {
        this.pfDeduction = pfDeduction;
    }

    public BigDecimal getEsiDeduction() {
        return esiDeduction;
    }

    public void setEsiDeduction(BigDecimal esiDeduction) {
        this.esiDeduction = esiDeduction;
    }

    public BigDecimal getTdsTaxDeduction() {
        return tdsTaxDeduction;
    }

    public void setTdsTaxDeduction(BigDecimal tdsTaxDeduction) {
        this.tdsTaxDeduction = tdsTaxDeduction;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay;
    }
}

