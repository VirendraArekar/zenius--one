package com.zenius.one.modules.payroll;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.payroll.PayrollService.FnfSettlementDto;
import com.zenius.one.modules.payroll.PayrollService.PayRunDto;
import com.zenius.one.modules.payroll.PayrollService.PayslipDto;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payroll")
@Tag(name = "Payroll", description = "Monthly payroll runs, statutory PF/ESI/TDS payslips, and F&F settlements")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/runs")
    @PreAuthorize("@authz.hasPermission('payroll:read')")
    public ApiResponse<List<PayRunDto>> listRuns() {
        return ApiResponse.ok(payrollService.listRuns());
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('payroll:write')")
    public ApiResponse<PayRunDto> processRun(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody RunBody body
    ) {
        return ApiResponse.ok(payrollService.processPayrollRun(principal, body.payMonth()));
    }

    @GetMapping("/payslips")
    @PreAuthorize("@authz.hasPermission('payroll:read')")
    public ApiResponse<List<PayslipDto>> listPayslips() {
        return ApiResponse.ok(payrollService.listPayslips());
    }

    @PostMapping("/employees/{employeeId}/fnf")
    @PreAuthorize("@authz.hasPermission('payroll:write')")
    public ApiResponse<FnfSettlementDto> processFnf(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID employeeId
    ) {
        return ApiResponse.ok(payrollService.processFullAndFinal(principal, employeeId));
    }

    public record RunBody(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "Format must be YYYY-MM") String payMonth) {
    }
}

