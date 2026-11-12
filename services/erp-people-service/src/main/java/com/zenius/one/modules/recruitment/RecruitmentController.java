package com.zenius.one.modules.recruitment;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.recruitment.RecruitmentService.CandidateDto;
import com.zenius.one.modules.recruitment.RecruitmentService.CreateCandidateRequest;
import com.zenius.one.modules.recruitment.RecruitmentService.CreateJobRequest;
import com.zenius.one.modules.recruitment.RecruitmentService.HireResult;
import com.zenius.one.modules.recruitment.RecruitmentService.InterviewDto;
import com.zenius.one.modules.recruitment.RecruitmentService.JobDto;
import com.zenius.one.modules.recruitment.RecruitmentService.ScheduleInterviewRequest;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruitment")
@Tag(name = "Recruitment ATS", description = "Job openings, applicant tracking pipeline, interviews, and hiring to HR")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @GetMapping("/jobs")
    @PreAuthorize("@authz.hasPermission('recruitment:read')")
    public ApiResponse<List<JobDto>> listJobs() {
        return ApiResponse.ok(recruitmentService.listJobs());
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('recruitment:write')")
    public ApiResponse<JobDto> createJob(@Valid @RequestBody JobBody body) {
        return ApiResponse.ok(recruitmentService.createJob(
                new CreateJobRequest(body.title(), body.department(), body.location(), body.employmentType(), body.openingsCount())
        ));
    }

    @GetMapping("/candidates")
    @PreAuthorize("@authz.hasPermission('recruitment:read')")
    public ApiResponse<List<CandidateDto>> listCandidates() {
        return ApiResponse.ok(recruitmentService.listCandidates());
    }

    @PostMapping("/candidates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('recruitment:write')")
    public ApiResponse<CandidateDto> addCandidate(@Valid @RequestBody CandidateBody body) {
        return ApiResponse.ok(recruitmentService.addCandidate(
                new CreateCandidateRequest(body.jobId(), body.fullName(), body.email(), body.phone(), body.skills(), body.experienceYears(), body.resumeSummary())
        ));
    }

    @PatchMapping("/candidates/{id}/stage")
    @PreAuthorize("@authz.hasPermission('recruitment:write')")
    public ApiResponse<CandidateDto> updateStage(@PathVariable UUID id, @Valid @RequestBody StageBody body) {
        return ApiResponse.ok(recruitmentService.updateStage(id, body.stage(), body.rating()));
    }

    @PostMapping("/candidates/{id}/hire")
    @PreAuthorize("@authz.hasPermission('recruitment:write')")
    public ApiResponse<HireResult> hireCandidate(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody HireBody body
    ) {
        return ApiResponse.ok(recruitmentService.hireCandidate(principal, id, body.baseMonthlySalary()));
    }

    @GetMapping("/interviews")
    @PreAuthorize("@authz.hasPermission('recruitment:read')")
    public ApiResponse<List<InterviewDto>> listInterviews() {
        return ApiResponse.ok(recruitmentService.listInterviews());
    }

    @PostMapping("/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('recruitment:write')")
    public ApiResponse<InterviewDto> scheduleInterview(@Valid @RequestBody InterviewBody body) {
        return ApiResponse.ok(recruitmentService.scheduleInterview(
                new ScheduleInterviewRequest(body.candidateId(), body.interviewerName(), body.scheduledAt(), body.mode())
        ));
    }

    public record JobBody(@NotBlank String title, String department, String location, String employmentType, Integer openingsCount) {
    }

    public record CandidateBody(@NotNull UUID jobId, @NotBlank String fullName, @NotBlank @Email String email, String phone, String skills, Integer experienceYears, String resumeSummary) {
    }

    public record StageBody(@NotBlank String stage, Integer rating) {
    }

    public record HireBody(@NotNull @DecimalMin("0.00") BigDecimal baseMonthlySalary) {
    }

    public record InterviewBody(@NotNull UUID candidateId, @NotBlank String interviewerName, Instant scheduledAt, String mode) {
    }
}

