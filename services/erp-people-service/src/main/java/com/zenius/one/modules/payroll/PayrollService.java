package com.zenius.one.modules.payroll;

import com.zenius.one.modules.hr.HrEmployeeEntity;
import com.zenius.one.modules.hr.HrEmployeeRepository;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

    private final PayRunRepository runRepository;
    private final PayPayslipRepository payslipRepository;
    private final HrEmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    public PayrollService(
            PayRunRepository runRepository,
            PayPayslipRepository payslipRepository,
            HrEmployeeRepository employeeRepository,
            AuditLogService auditLogService
    ) {
        this.runRepository = runRepository;
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PayRunDto> listRuns() {
        UUID orgId = TenantContext.requireOrganizationId();
        return runRepository.findByOrganizationIdOrderByPayMonthDesc(orgId).stream()
                .map(r -> new PayRunDto(r.getId(), r.getPayMonth(), r.getEmployeesCount(), r.getTotalGross(), r.getTotalDeductions(), r.getTotalNetPay(), r.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayslipDto> listPayslips() {
        UUID orgId = TenantContext.requireOrganizationId();
        return payslipRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(p -> new PayslipDto(
                        p.getId(), p.getPayrollRunId(), p.getEmployeeId(), p.getEmployeeName(), p.getPayMonth(),
                        p.getBasicPay(), p.getHraAllowance(), p.getSpecialAllowance(), p.getGrossPay(),
                        p.getPfDeduction(), p.getEsiDeduction(), p.getTdsTaxDeduction(), p.getTotalDeductions(), p.getNetPay()
                ))
                .toList();
    }

    @Transactional
    public PayRunDto processPayrollRun(ZeniusUserPrincipal principal, String payMonth) {
        UUID orgId = TenantContext.requireOrganizationId();
        String month = payMonth.trim();
        if (runRepository.existsByOrganizationIdAndPayMonth(orgId, month)) {
            throw new IllegalStateException("Payroll already processed for month: " + month);
        }

        List<HrEmployeeEntity> employees = employeeRepository.findByOrganizationIdAndStatus(orgId, "ACTIVE");
        if (employees.isEmpty()) {
            throw new IllegalStateException("No active employees found to run payroll");
        }

        PayRunEntity run = new PayRunEntity();
        run.setOrganizationId(orgId);
        run.setPayMonth(month);
        run.setStatus("PROCESSED");
        run = runRepository.save(run);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDed = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (HrEmployeeEntity emp : employees) {
            BigDecimal gross = emp.getBaseMonthlySalary();
            BigDecimal basic = gross.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal hra = gross.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal special = gross.subtract(basic).subtract(hra);

            BigDecimal pf = basic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal esi = gross.compareTo(new BigDecimal("21000")) <= 0
                    ? gross.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal tds = BigDecimal.ZERO;
            if (gross.compareTo(new BigDecimal("50000")) > 0) {
                tds = gross.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            } else if (gross.compareTo(new BigDecimal("25000")) > 0) {
                tds = gross.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal deductions = pf.add(esi).add(tds);
            BigDecimal net = gross.subtract(deductions);

            PayPayslipEntity slip = new PayPayslipEntity();
            slip.setOrganizationId(orgId);
            slip.setPayrollRunId(run.getId());
            slip.setEmployeeId(emp.getId());
            slip.setEmployeeName(emp.getFullName());
            slip.setPayMonth(month);
            slip.setBasicPay(basic);
            slip.setHraAllowance(hra);
            slip.setSpecialAllowance(special);
            slip.setGrossPay(gross);
            slip.setPfDeduction(pf);
            slip.setEsiDeduction(esi);
            slip.setTdsTaxDeduction(tds);
            slip.setTotalDeductions(deductions);
            slip.setNetPay(net);
            payslipRepository.save(slip);

            totalGross = totalGross.add(gross);
            totalDed = totalDed.add(deductions);
            totalNet = totalNet.add(net);
        }

        run.setEmployeesCount(employees.size());
        run.setTotalGross(totalGross);
        run.setTotalDeductions(totalDed);
        run.setTotalNetPay(totalNet);
        run = runRepository.save(run);

        auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                "PAYROLL_RUN_PROCESSED", "PAYROLL_RUN", run.getId().toString(),
                "Processed payroll for " + month + " (" + employees.size() + " employees, net " + totalNet + ")", "SUCCESS");
        return new PayRunDto(run.getId(), run.getPayMonth(), run.getEmployeesCount(), run.getTotalGross(), run.getTotalDeductions(), run.getTotalNetPay(), run.getStatus());
    }

    @Transactional
    public FnfSettlementDto processFullAndFinal(ZeniusUserPrincipal principal, UUID employeeId) {
        UUID orgId = TenantContext.requireOrganizationId();
        HrEmployeeEntity emp = employeeRepository.findByIdAndOrganizationId(employeeId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        BigDecimal dailyRate = emp.getBaseMonthlySalary().divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        BigDecimal leaveEncashment = dailyRate.multiply(BigDecimal.valueOf(emp.getAnnualLeaveBalance())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSettlement = emp.getBaseMonthlySalary().add(leaveEncashment);

        emp.setStatus("OFFBOARDED");
        employeeRepository.save(emp);

        auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                "HR_FNF_PROCESSED", "HR_EMPLOYEE", emp.getId().toString(),
                "Processed F&F settlement for " + emp.getFullName() + " (" + totalSettlement + ")", "SUCCESS");
        return new FnfSettlementDto(emp.getId(), emp.getFullName(), emp.getBaseMonthlySalary(), leaveEncashment, totalSettlement, emp.getStatus());
    }

    public record PayRunDto(UUID id, String payMonth, Integer employeesCount, BigDecimal totalGross, BigDecimal totalDeductions, BigDecimal totalNetPay, String status) {
    }

    public record PayslipDto(UUID id, UUID payrollRunId, UUID employeeId, String employeeName, String payMonth, BigDecimal basicPay, BigDecimal hraAllowance, BigDecimal specialAllowance, BigDecimal grossPay, BigDecimal pfDeduction, BigDecimal esiDeduction, BigDecimal tdsTaxDeduction, BigDecimal totalDeductions, BigDecimal netPay) {
    }

    public record FnfSettlementDto(UUID employeeId, String employeeName, BigDecimal baseSalary, BigDecimal leaveEncashment, BigDecimal totalSettlement, String employeeStatus) {
    }
}

