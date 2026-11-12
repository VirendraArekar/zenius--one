package com.zenius.one.modules.recruitment;

import com.zenius.one.modules.hr.HrService;
import com.zenius.one.modules.hr.HrService.CreateEmployeeRequest;
import com.zenius.one.modules.hr.HrService.EmployeeDto;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentService {

    private final RecJobRepository jobRepository;
    private final RecCandidateRepository candidateRepository;
    private final RecInterviewRepository interviewRepository;
    private final HrService hrService;

    public RecruitmentService(
            RecJobRepository jobRepository,
            RecCandidateRepository candidateRepository,
            RecInterviewRepository interviewRepository,
            HrService hrService
    ) {
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.interviewRepository = interviewRepository;
        this.hrService = hrService;
    }

    @Transactional(readOnly = true)
    public List<JobDto> listJobs() {
        UUID orgId = TenantContext.requireOrganizationId();
        return jobRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(j -> new JobDto(j.getId(), j.getJobCode(), j.getTitle(), j.getDepartment(), j.getLocation(), j.getEmploymentType(), j.getOpeningsCount(), j.getStatus()))
                .toList();
    }

    @Transactional
    public JobDto createJob(CreateJobRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        long seq = jobRepository.countByOrganizationId(orgId) + 1;
        RecJobEntity job = new RecJobEntity();
        job.setOrganizationId(orgId);
        job.setJobCode(String.format("JOB-%04d", seq));
        job.setTitle(req.title().trim());
        job.setDepartment(req.department());
        job.setLocation(req.location());
        job.setEmploymentType(req.employmentType() != null ? req.employmentType().trim().toUpperCase() : "FULL_TIME");
        job.setOpeningsCount(req.openingsCount() != null ? req.openingsCount() : 1);
        job.setStatus("OPEN");
        job = jobRepository.save(job);
        return new JobDto(job.getId(), job.getJobCode(), job.getTitle(), job.getDepartment(), job.getLocation(), job.getEmploymentType(), job.getOpeningsCount(), job.getStatus());
    }

    @Transactional(readOnly = true)
    public List<CandidateDto> listCandidates() {
        UUID orgId = TenantContext.requireOrganizationId();
        return candidateRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toCandidateDto)
                .toList();
    }

    @Transactional
    public CandidateDto addCandidate(CreateCandidateRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        jobRepository.findByIdAndOrganizationId(req.jobId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Job opening not found in current organization"));

        RecCandidateEntity c = new RecCandidateEntity();
        c.setOrganizationId(orgId);
        c.setJobId(req.jobId());
        c.setFullName(req.fullName().trim());
        c.setEmail(req.email().trim().toLowerCase());
        c.setPhone(req.phone());
        c.setSkills(req.skills());
        c.setExperienceYears(req.experienceYears() != null ? req.experienceYears() : 0);
        c.setResumeSummary(req.resumeSummary());
        c.setStage("APPLIED");
        return toCandidateDto(candidateRepository.save(c));
    }

    @Transactional
    public CandidateDto updateStage(UUID candidateId, String stage, Integer rating) {
        UUID orgId = TenantContext.requireOrganizationId();
        RecCandidateEntity c = candidateRepository.findByIdAndOrganizationId(candidateId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        c.setStage(stage.trim().toUpperCase());
        if (rating != null) {
            c.setRating(rating);
        }
        return toCandidateDto(candidateRepository.save(c));
    }

    @Transactional
    public HireResult hireCandidate(ZeniusUserPrincipal principal, UUID candidateId, BigDecimal monthlySalary) {
        UUID orgId = TenantContext.requireOrganizationId();
        RecCandidateEntity c = candidateRepository.findByIdAndOrganizationId(candidateId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        if ("HIRED".equals(c.getStage()) && c.getHiredEmployeeId() != null) {
            throw new IllegalStateException("Candidate already hired");
        }
        RecJobEntity job = jobRepository.findByIdAndOrganizationId(c.getJobId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Job opening not found"));

        EmployeeDto emp = hrService.createEmployee(principal, new CreateEmployeeRequest(
                c.getFullName(),
                c.getEmail(),
                job.getTitle(),
                job.getDepartment(),
                job.getLocation(),
                null,
                LocalDate.now(),
                job.getEmploymentType(),
                monthlySalary
        ));

        c.setStage("HIRED");
        c.setHiredEmployeeId(emp.id());
        candidateRepository.save(c);
        return new HireResult(c.getId(), emp.id(), emp.employeeCode());
    }

    @Transactional(readOnly = true)
    public List<InterviewDto> listInterviews() {
        UUID orgId = TenantContext.requireOrganizationId();
        return interviewRepository.findByOrganizationIdOrderByScheduledAtAsc(orgId).stream()
                .map(i -> new InterviewDto(i.getId(), i.getCandidateId(), i.getInterviewerName(), i.getScheduledAt(), i.getMode(), i.getRecommendation()))
                .toList();
    }

    @Transactional
    public InterviewDto scheduleInterview(ScheduleInterviewRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        candidateRepository.findByIdAndOrganizationId(req.candidateId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found in current organization"));
        RecInterviewEntity i = new RecInterviewEntity();
        i.setOrganizationId(orgId);
        i.setCandidateId(req.candidateId());
        i.setInterviewerName(req.interviewerName().trim());
        i.setScheduledAt(req.scheduledAt() != null ? req.scheduledAt() : Instant.now().plusSeconds(86400));
        i.setMode(req.mode() != null ? req.mode().trim().toUpperCase() : "VIDEO");
        i = interviewRepository.save(i);
        return new InterviewDto(i.getId(), i.getCandidateId(), i.getInterviewerName(), i.getScheduledAt(), i.getMode(), i.getRecommendation());
    }

    private CandidateDto toCandidateDto(RecCandidateEntity c) {
        return new CandidateDto(c.getId(), c.getJobId(), c.getFullName(), c.getEmail(), c.getSkills(), c.getExperienceYears(), c.getStage(), c.getRating(), c.getHiredEmployeeId());
    }

    public record JobDto(UUID id, String jobCode, String title, String department, String location, String employmentType, Integer openingsCount, String status) {
    }

    public record CreateJobRequest(String title, String department, String location, String employmentType, Integer openingsCount) {
    }

    public record CreateCandidateRequest(UUID jobId, String fullName, String email, String phone, String skills, Integer experienceYears, String resumeSummary) {
    }

    public record CandidateDto(UUID id, UUID jobId, String fullName, String email, String skills, Integer experienceYears, String stage, Integer rating, UUID hiredEmployeeId) {
    }

    public record HireResult(UUID candidateId, UUID employeeId, String employeeCode) {
    }

    public record ScheduleInterviewRequest(UUID candidateId, String interviewerName, Instant scheduledAt, String mode) {
    }

    public record InterviewDto(UUID id, UUID candidateId, String interviewerName, Instant scheduledAt, String mode, String recommendation) {
    }
}

