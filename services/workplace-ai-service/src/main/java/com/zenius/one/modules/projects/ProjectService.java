package com.zenius.one.modules.projects;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjProjectRepository projectRepository;
    private final ProjSprintRepository sprintRepository;
    private final ProjTaskRepository taskRepository;
    private final ProjTimeLogRepository timeLogRepository;

    public ProjectService(
            ProjProjectRepository projectRepository,
            ProjSprintRepository sprintRepository,
            ProjTaskRepository taskRepository,
            ProjTimeLogRepository timeLogRepository
    ) {
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.timeLogRepository = timeLogRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> listProjects() {
        UUID orgId = TenantContext.requireOrganizationId();
        return projectRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(p -> new ProjectDto(p.getId(), p.getProjectCode(), p.getName(), p.getClientName(), p.getStatus(), p.getBudgetAmount(), p.getStartDate(), p.getDueDate()))
                .toList();
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        long seq = projectRepository.countByOrganizationId(orgId) + 1;
        ProjProjectEntity p = new ProjProjectEntity();
        p.setOrganizationId(orgId);
        p.setProjectCode(String.format("PRJ-%03d", seq));
        p.setName(req.name().trim());
        p.setClientName(req.clientName());
        p.setStatus("ACTIVE");
        p.setBudgetAmount(req.budgetAmount() != null ? req.budgetAmount() : BigDecimal.ZERO);
        p.setStartDate(req.startDate() != null ? req.startDate() : LocalDate.now());
        p.setDueDate(req.dueDate());
        p = projectRepository.save(p);
        return new ProjectDto(p.getId(), p.getProjectCode(), p.getName(), p.getClientName(), p.getStatus(), p.getBudgetAmount(), p.getStartDate(), p.getDueDate());
    }

    @Transactional(readOnly = true)
    public List<SprintDto> listSprints(UUID projectId) {
        UUID orgId = TenantContext.requireOrganizationId();
        return sprintRepository.findByOrganizationIdAndProjectIdOrderByStartDateAsc(orgId, projectId).stream()
                .map(s -> new SprintDto(s.getId(), s.getProjectId(), s.getName(), s.getGoal(), s.getStartDate(), s.getEndDate(), s.getStatus()))
                .toList();
    }

    @Transactional
    public SprintDto createSprint(CreateSprintRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        projectRepository.findByIdAndOrganizationId(req.projectId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found in current organization"));
        ProjSprintEntity s = new ProjSprintEntity();
        s.setOrganizationId(orgId);
        s.setProjectId(req.projectId());
        s.setName(req.name().trim());
        s.setGoal(req.goal());
        s.setStartDate(req.startDate() != null ? req.startDate() : LocalDate.now());
        s.setEndDate(req.endDate() != null ? req.endDate() : LocalDate.now().plusWeeks(2));
        s.setStatus("ACTIVE");
        s = sprintRepository.save(s);
        return new SprintDto(s.getId(), s.getProjectId(), s.getName(), s.getGoal(), s.getStartDate(), s.getEndDate(), s.getStatus());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> listTasks(UUID projectId) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<ProjTaskEntity> list = projectId != null
                ? taskRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(orgId, projectId)
                : taskRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return list.stream().map(this::toTaskDto).toList();
    }

    @Transactional
    public TaskDto createTask(CreateTaskRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ProjProjectEntity proj = projectRepository.findByIdAndOrganizationId(req.projectId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found in current organization"));
        long seq = taskRepository.countByOrganizationIdAndProjectId(orgId, proj.getId()) + 1;
        ProjTaskEntity t = new ProjTaskEntity();
        t.setOrganizationId(orgId);
        t.setProjectId(proj.getId());
        t.setSprintId(req.sprintId());
        t.setTaskKey(proj.getProjectCode() + "-" + seq);
        t.setTitle(req.title().trim());
        t.setTaskType(req.taskType() != null ? req.taskType().trim().toUpperCase() : "TASK");
        t.setPriority(req.priority() != null ? req.priority().trim().toUpperCase() : "MEDIUM");
        t.setStatus("TODO");
        t.setStoryPoints(req.storyPoints() != null ? req.storyPoints() : 1);
        t.setAssigneeName(req.assigneeName());
        t.setDueDate(req.dueDate());
        return toTaskDto(taskRepository.save(t));
    }

    @Transactional
    public TaskDto updateTaskStatus(UUID taskId, String status) {
        UUID orgId = TenantContext.requireOrganizationId();
        ProjTaskEntity t = taskRepository.findByIdAndOrganizationId(taskId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        t.setStatus(status.trim().toUpperCase());
        return toTaskDto(taskRepository.save(t));
    }

    @Transactional
    public TimeLogDto logTime(ZeniusUserPrincipal principal, LogTimeRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ProjTaskEntity task = taskRepository.findByIdAndOrganizationId(req.taskId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + req.taskId()));
        ProjTimeLogEntity log = new ProjTimeLogEntity();
        log.setOrganizationId(orgId);
        log.setProjectId(task.getProjectId());
        log.setTaskId(task.getId());
        log.setMemberName(principal.getFullName());
        log.setMinutesLogged(req.minutesLogged());
        log.setBillable(req.billable());
        log.setWorkDate(req.workDate() != null ? req.workDate() : LocalDate.now());
        log.setNotes(req.notes());
        log = timeLogRepository.save(log);

        task.setLoggedMinutes(task.getLoggedMinutes() + req.minutesLogged());
        taskRepository.save(task);
        return new TimeLogDto(log.getId(), log.getProjectId(), log.getTaskId(), log.getMemberName(), log.getMinutesLogged(), log.isBillable(), log.getWorkDate(), log.getNotes());
    }

    @Transactional(readOnly = true)
    public List<TimeLogDto> listTimeLogs() {
        UUID orgId = TenantContext.requireOrganizationId();
        return timeLogRepository.findByOrganizationIdOrderByWorkDateDescCreatedAtDesc(orgId).stream()
                .map(l -> new TimeLogDto(l.getId(), l.getProjectId(), l.getTaskId(), l.getMemberName(), l.getMinutesLogged(), l.isBillable(), l.getWorkDate(), l.getNotes()))
                .toList();
    }

    private TaskDto toTaskDto(ProjTaskEntity t) {
        return new TaskDto(t.getId(), t.getProjectId(), t.getSprintId(), t.getTaskKey(), t.getTitle(), t.getTaskType(), t.getPriority(), t.getStatus(), t.getStoryPoints(), t.getAssigneeName(), t.getDueDate(), t.getLoggedMinutes());
    }

    public record ProjectDto(UUID id, String projectCode, String name, String clientName, String status, BigDecimal budgetAmount, LocalDate startDate, LocalDate dueDate) {
    }

    public record CreateProjectRequest(String name, String clientName, BigDecimal budgetAmount, LocalDate startDate, LocalDate dueDate) {
    }

    public record SprintDto(UUID id, UUID projectId, String name, String goal, LocalDate startDate, LocalDate endDate, String status) {
    }

    public record CreateSprintRequest(UUID projectId, String name, String goal, LocalDate startDate, LocalDate endDate) {
    }

    public record TaskDto(UUID id, UUID projectId, UUID sprintId, String taskKey, String title, String taskType, String priority, String status, Integer storyPoints, String assigneeName, LocalDate dueDate, Integer loggedMinutes) {
    }

    public record CreateTaskRequest(UUID projectId, UUID sprintId, String title, String taskType, String priority, Integer storyPoints, String assigneeName, LocalDate dueDate) {
    }

    public record LogTimeRequest(UUID taskId, int minutesLogged, boolean billable, LocalDate workDate, String notes) {
    }

    public record TimeLogDto(UUID id, UUID projectId, UUID taskId, String memberName, Integer minutesLogged, boolean billable, LocalDate workDate, String notes) {
    }
}

