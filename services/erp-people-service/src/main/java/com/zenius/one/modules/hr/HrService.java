package com.zenius.one.modules.hr;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrService {

    private final HrEmployeeRepository employeeRepository;
    private final HrAttendanceRepository attendanceRepository;
    private final HrLeaveRequestRepository leaveRepository;
    private final HrPerformanceGoalRepository goalRepository;
    private final AuditLogService auditLogService;

    public HrService(
            HrEmployeeRepository employeeRepository,
            HrAttendanceRepository attendanceRepository,
            HrLeaveRequestRepository leaveRepository,
            HrPerformanceGoalRepository goalRepository,
            AuditLogService auditLogService
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.goalRepository = goalRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> listEmployees() {
        UUID orgId = TenantContext.requireOrganizationId();
        return employeeRepository.findByOrganizationIdOrderByEmployeeCodeAsc(orgId).stream()
                .map(this::toEmpDto)
                .toList();
    }

    @Transactional
    public EmployeeDto createEmployee(ZeniusUserPrincipal principal, CreateEmployeeRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        String email = req.workEmail().trim().toLowerCase();
        if (employeeRepository.existsByOrganizationIdAndWorkEmailIgnoreCase(orgId, email)) {
            throw new IllegalStateException("Employee already exists with email: " + email);
        }
        long seq = employeeRepository.countByOrganizationId(orgId) + 1;
        HrEmployeeEntity emp = new HrEmployeeEntity();
        emp.setOrganizationId(orgId);
        emp.setEmployeeCode(String.format("EMP-%04d", seq));
        emp.setFullName(req.fullName().trim());
        emp.setWorkEmail(email);
        emp.setDesignation(req.designation().trim());
        emp.setDepartmentName(req.departmentName());
        emp.setBranchName(req.branchName());
        emp.setManagerEmployeeId(req.managerEmployeeId());
        emp.setJoiningDate(req.joiningDate() != null ? req.joiningDate() : LocalDate.now());
        emp.setEmploymentType(req.employmentType() != null ? req.employmentType().trim().toUpperCase() : "FULL_TIME");
        emp.setStatus("ACTIVE");
        emp.setBaseMonthlySalary(req.baseMonthlySalary() != null ? req.baseMonthlySalary() : BigDecimal.ZERO);
        emp = employeeRepository.save(emp);

        if (principal != null) {
            auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                    "HR_EMPLOYEE_CREATED", "HR_EMPLOYEE", emp.getId().toString(),
                    "Onboarded employee " + emp.getEmployeeCode() + " - " + emp.getFullName(), "SUCCESS");
        }
        return toEmpDto(emp);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto> listAttendance() {
        UUID orgId = TenantContext.requireOrganizationId();
        return attendanceRepository.findByOrganizationIdOrderByWorkDateDescCheckInAtDesc(orgId).stream()
                .map(this::toAttDto)
                .toList();
    }

    @Transactional
    public AttendanceDto checkIn(CheckInRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        employeeRepository.findByIdAndOrganizationId(req.employeeId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found in current organization"));
        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByOrganizationIdAndEmployeeIdAndWorkDate(orgId, req.employeeId(), today).isPresent()) {
            throw new IllegalStateException("Employee already checked in today");
        }
        HrAttendanceEntity att = new HrAttendanceEntity();
        att.setOrganizationId(orgId);
        att.setEmployeeId(req.employeeId());
        att.setWorkDate(today);
        att.setCheckInAt(Instant.now());
        att.setWorkMode(req.workMode() != null ? req.workMode().trim().toUpperCase() : "OFFICE");
        att.setLatitude(req.latitude());
        att.setLongitude(req.longitude());
        att.setLocationLabel(req.locationLabel() != null && !req.locationLabel().isBlank()
                ? req.locationLabel().trim()
                : resolveLocationLabel(req.latitude(), req.longitude(), req.workMode()));
        att.setStatus("PRESENT");
        return toAttDto(attendanceRepository.save(att));
    }

    @Transactional
    public AttendanceDto checkOut(UUID employeeId, Double latitude, Double longitude) {
        UUID orgId = TenantContext.requireOrganizationId();
        HrAttendanceEntity att = attendanceRepository
                .findByOrganizationIdAndEmployeeIdAndWorkDate(orgId, employeeId, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("No active check-in found for today"));
        Instant now = Instant.now();
        att.setCheckOutAt(now);
        if (latitude != null) att.setCheckOutLatitude(latitude);
        if (longitude != null) att.setCheckOutLongitude(longitude);
        int mins = (int) Math.max(0, Duration.between(att.getCheckInAt(), now).toMinutes());
        att.setWorkedMinutes(mins);
        att.setOvertimeMinutes(Math.max(0, mins - 480));
        return toAttDto(attendanceRepository.save(att));
    }

    private String resolveLocationLabel(Double lat, Double lng, String mode) {
        if ("REMOTE".equalsIgnoreCase(mode)) {
            return lat != null ? String.format("Remote Punch (%.4f, %.4f)", lat, lng) : "Remote Work";
        }
        return lat != null ? String.format("Office HQ (%.4f, %.4f)", lat, lng) : "Office HQ Geo-fenced";
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listLeaves() {
        UUID orgId = TenantContext.requireOrganizationId();
        return leaveRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toLeaveDto)
                .toList();
    }

    @Transactional
    public LeaveRequestDto submitLeave(CreateLeaveRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        HrEmployeeEntity emp = employeeRepository.findByIdAndOrganizationId(req.employeeId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found in current organization"));
        if (req.endDate().isBefore(req.startDate())) {
            throw new IllegalArgumentException("End date cannot precede start date");
        }
        int days = (int) ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;
        if ("ANNUAL".equalsIgnoreCase(req.leaveType()) && emp.getAnnualLeaveBalance() < days) {
            throw new IllegalStateException("Insufficient annual leave balance (" + emp.getAnnualLeaveBalance() + " days)");
        }
        HrLeaveRequestEntity leave = new HrLeaveRequestEntity();
        leave.setOrganizationId(orgId);
        leave.setEmployeeId(emp.getId());
        leave.setLeaveType(req.leaveType().trim().toUpperCase());
        leave.setStartDate(req.startDate());
        leave.setEndDate(req.endDate());
        leave.setDaysCount(days);
        leave.setReason(req.reason());
        leave.setStatus("PENDING");
        return toLeaveDto(leaveRepository.save(leave));
    }

    @Transactional
    public LeaveRequestDto decideLeave(ZeniusUserPrincipal principal, UUID leaveId, boolean approved) {
        UUID orgId = TenantContext.requireOrganizationId();
        HrLeaveRequestEntity leave = leaveRepository.findByIdAndOrganizationId(leaveId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + leaveId));
        if (!"PENDING".equals(leave.getStatus())) {
            throw new IllegalStateException("Leave request already decided");
        }
        if (approved && "ANNUAL".equals(leave.getLeaveType())) {
            HrEmployeeEntity emp = employeeRepository.findByIdAndOrganizationId(leave.getEmployeeId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
            emp.setAnnualLeaveBalance(Math.max(0, emp.getAnnualLeaveBalance() - leave.getDaysCount()));
            employeeRepository.save(emp);
        }
        leave.setStatus(approved ? "APPROVED" : "REJECTED");
        leave.setReviewedByUserId(principal != null ? principal.getUserId() : null);
        leave.setReviewedAt(Instant.now());
        return toLeaveDto(leaveRepository.save(leave));
    }

    @Transactional
    public LeaveRequestDto cancelLeave(UUID leaveId) {
        UUID orgId = TenantContext.requireOrganizationId();
        HrLeaveRequestEntity leave = leaveRepository.findByIdAndOrganizationId(leaveId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + leaveId));
        if (!"PENDING".equals(leave.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be cancelled");
        }
        leave.setStatus("CANCELLED");
        return toLeaveDto(leaveRepository.save(leave));
    }

    @Transactional(readOnly = true)
    public List<GoalDto> listGoals() {
        UUID orgId = TenantContext.requireOrganizationId();
        return goalRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(g -> new GoalDto(g.getId(), g.getEmployeeId(), g.getTitle(), g.getTargetMetric(), g.getProgressPercent(), g.getReviewStatus()))
                .toList();
    }

    @Transactional
    public GoalDto updateGoalProgress(UUID goalId, int progressPercent) {
        UUID orgId = TenantContext.requireOrganizationId();
        HrPerformanceGoalEntity g = goalRepository.findById(goalId)
                .filter(goal -> goal.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        int pct = Math.clamp(progressPercent, 0, 100);
        g.setProgressPercent(pct);
        g.setReviewStatus(pct >= 100 ? "COMPLETED" : "IN_PROGRESS");
        g = goalRepository.save(g);
        return new GoalDto(g.getId(), g.getEmployeeId(), g.getTitle(), g.getTargetMetric(), g.getProgressPercent(), g.getReviewStatus());
    }

    @Transactional
    public GoalDto createGoal(CreateGoalRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        employeeRepository.findByIdAndOrganizationId(req.employeeId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found in current organization"));
        HrPerformanceGoalEntity g = new HrPerformanceGoalEntity();
        g.setOrganizationId(orgId);
        g.setEmployeeId(req.employeeId());
        g.setTitle(req.title().trim());
        g.setTargetMetric(req.targetMetric().trim());
        g.setProgressPercent(req.progressPercent() != null ? req.progressPercent() : 0);
        g.setReviewStatus("IN_PROGRESS");
        g = goalRepository.save(g);
        return new GoalDto(g.getId(), g.getEmployeeId(), g.getTitle(), g.getTargetMetric(), g.getProgressPercent(), g.getReviewStatus());
    }

    private EmployeeDto toEmpDto(HrEmployeeEntity e) {
        return new EmployeeDto(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getWorkEmail(), e.getDesignation(), e.getDepartmentName(), e.getBranchName(), e.getJoiningDate(), e.getEmploymentType(), e.getStatus(), e.getBaseMonthlySalary(), e.getAnnualLeaveBalance());
    }

    private AttendanceDto toAttDto(HrAttendanceEntity a) {
        return new AttendanceDto(
                a.getId(), a.getEmployeeId(), a.getWorkDate(), a.getCheckInAt(), a.getCheckOutAt(),
                a.getWorkMode(), a.getLatitude(), a.getLongitude(), a.getCheckOutLatitude(), a.getCheckOutLongitude(),
                a.getLocationLabel(), a.getWorkedMinutes(), a.getOvertimeMinutes(), a.getStatus()
        );
    }

    private LeaveRequestDto toLeaveDto(HrLeaveRequestEntity l) {
        return new LeaveRequestDto(l.getId(), l.getEmployeeId(), l.getLeaveType(), l.getStartDate(), l.getEndDate(), l.getDaysCount(), l.getReason(), l.getStatus());
    }

    public record CreateEmployeeRequest(String fullName, String workEmail, String designation, String departmentName, String branchName, UUID managerEmployeeId, LocalDate joiningDate, String employmentType, BigDecimal baseMonthlySalary) {
    }

    public record EmployeeDto(UUID id, String employeeCode, String fullName, String workEmail, String designation, String departmentName, String branchName, LocalDate joiningDate, String employmentType, String status, BigDecimal baseMonthlySalary, Integer annualLeaveBalance) {
    }

    public record CheckInRequest(UUID employeeId, String workMode, Double latitude, Double longitude, String locationLabel) {
    }

    public record AttendanceDto(
            UUID id,
            UUID employeeId,
            LocalDate workDate,
            Instant checkInAt,
            Instant checkOutAt,
            String workMode,
            Double latitude,
            Double longitude,
            Double checkOutLatitude,
            Double checkOutLongitude,
            String locationLabel,
            Integer workedMinutes,
            Integer overtimeMinutes,
            String status
    ) {
    }

    public record CreateLeaveRequest(UUID employeeId, String leaveType, LocalDate startDate, LocalDate endDate, String reason) {
    }

    public record LeaveRequestDto(UUID id, UUID employeeId, String leaveType, LocalDate startDate, LocalDate endDate, Integer daysCount, String reason, String status) {
    }

    public record CreateGoalRequest(UUID employeeId, String title, String targetMetric, Integer progressPercent) {
    }

    public record GoalDto(UUID id, UUID employeeId, String title, String targetMetric, Integer progressPercent, String reviewStatus) {
    }
}

