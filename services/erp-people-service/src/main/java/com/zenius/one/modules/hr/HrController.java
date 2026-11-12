package com.zenius.one.modules.hr;

import com.zenius.one.modules.hr.HrService.AttendanceDto;
import com.zenius.one.modules.hr.HrService.CheckInRequest;
import com.zenius.one.modules.hr.HrService.CreateEmployeeRequest;
import com.zenius.one.modules.hr.HrService.CreateGoalRequest;
import com.zenius.one.modules.hr.HrService.CreateLeaveRequest;
import com.zenius.one.modules.hr.HrService.EmployeeDto;
import com.zenius.one.modules.hr.HrService.GoalDto;
import com.zenius.one.modules.hr.HrService.LeaveRequestDto;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/hr")
@Tag(name = "Human Resource Management", description = "Employees, geo-attendance, leave workflows, and performance goals")
public class HrController {

    private final HrService hrService;

    public HrController(HrService hrService) {
        this.hrService = hrService;
    }

    @GetMapping("/employees")
    @PreAuthorize("@authz.hasPermission('hr:read')")
    public ApiResponse<List<EmployeeDto>> listEmployees() {
        return ApiResponse.ok(hrService.listEmployees());
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<EmployeeDto> createEmployee(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody EmployeeBody body
    ) {
        return ApiResponse.ok(hrService.createEmployee(
                principal,
                new CreateEmployeeRequest(
                        body.fullName(), body.workEmail(), body.designation(), body.departmentName(),
                        body.branchName(), body.managerEmployeeId(), body.joiningDate(), body.employmentType(), body.baseMonthlySalary()
                )
        ));
    }

    @GetMapping("/attendance")
    @PreAuthorize("@authz.hasPermission('hr:read')")
    public ApiResponse<List<AttendanceDto>> listAttendance() {
        return ApiResponse.ok(hrService.listAttendance());
    }

    @PostMapping("/attendance/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<AttendanceDto> checkIn(@Valid @RequestBody CheckInBody body) {
        return ApiResponse.ok(hrService.checkIn(new CheckInRequest(body.employeeId(), body.workMode(), body.latitude(), body.longitude(), body.locationLabel())));
    }

    @PostMapping("/attendance/{employeeId}/check-out")
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<AttendanceDto> checkOut(
            @PathVariable UUID employeeId,
            @RequestBody(required = false) CheckOutBody body
    ) {
        Double lat = body != null ? body.latitude() : null;
        Double lng = body != null ? body.longitude() : null;
        return ApiResponse.ok(hrService.checkOut(employeeId, lat, lng));
    }

    @GetMapping("/leaves")
    @PreAuthorize("@authz.hasPermission('hr:read')")
    public ApiResponse<List<LeaveRequestDto>> listLeaves() {
        return ApiResponse.ok(hrService.listLeaves());
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<LeaveRequestDto> submitLeave(@Valid @RequestBody LeaveBody body) {
        return ApiResponse.ok(hrService.submitLeave(new CreateLeaveRequest(body.employeeId(), body.leaveType(), body.startDate(), body.endDate(), body.reason())));
    }

    @PostMapping("/leaves/{id}/decision")
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<LeaveRequestDto> decideLeave(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody LeaveDecisionBody body
    ) {
        return ApiResponse.ok(hrService.decideLeave(principal, id, body.approved()));
    }

    @PostMapping("/leaves/{id}/cancel")
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<LeaveRequestDto> cancelLeave(@PathVariable UUID id) {
        return ApiResponse.ok(hrService.cancelLeave(id));
    }

    @GetMapping("/goals")
    @PreAuthorize("@authz.hasPermission('hr:read')")
    public ApiResponse<List<GoalDto>> listGoals() {
        return ApiResponse.ok(hrService.listGoals());
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<GoalDto> createGoal(@Valid @RequestBody GoalBody body) {
        return ApiResponse.ok(hrService.createGoal(new CreateGoalRequest(body.employeeId(), body.title(), body.targetMetric(), body.progressPercent())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/goals/{id}/progress")
    @PreAuthorize("@authz.hasPermission('hr:write')")
    public ApiResponse<GoalDto> updateGoalProgress(
            @PathVariable UUID id,
            @RequestBody GoalProgressBody body
    ) {
        return ApiResponse.ok(hrService.updateGoalProgress(id, body.progressPercent()));
    }

    public record GoalProgressBody(int progressPercent) {}

    public record EmployeeBody(
            @NotBlank String fullName,
            @NotBlank @Email String workEmail,
            @NotBlank String designation,
            String departmentName,
            String branchName,
            UUID managerEmployeeId,
            LocalDate joiningDate,
            String employmentType,
            @DecimalMin("0.00") BigDecimal baseMonthlySalary
    ) {
    }

    public record CheckInBody(@NotNull UUID employeeId, String workMode, Double latitude, Double longitude, String locationLabel) {
    }

    public record CheckOutBody(Double latitude, Double longitude) {
    }

    public record LeaveBody(@NotNull UUID employeeId, @NotBlank String leaveType, @NotNull LocalDate startDate, @NotNull LocalDate endDate, String reason) {
    }

    public record LeaveDecisionBody(boolean approved) {
    }

    public record GoalBody(@NotNull UUID employeeId, @NotBlank String title, @NotBlank String targetMetric, Integer progressPercent) {
    }
}

