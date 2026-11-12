package com.zenius.one.modules.projects;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.projects.ProjectService.CreateProjectRequest;
import com.zenius.one.modules.projects.ProjectService.CreateSprintRequest;
import com.zenius.one.modules.projects.ProjectService.CreateTaskRequest;
import com.zenius.one.modules.projects.ProjectService.LogTimeRequest;
import com.zenius.one.modules.projects.ProjectService.ProjectDto;
import com.zenius.one.modules.projects.ProjectService.SprintDto;
import com.zenius.one.modules.projects.ProjectService.TaskDto;
import com.zenius.one.modules.projects.ProjectService.TimeLogDto;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Management", description = "Projects, sprints, Kanban tasks, bugs, and timesheets")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('projects:read')")
    public ApiResponse<List<ProjectDto>> listProjects() {
        return ApiResponse.ok(projectService.listProjects());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('projects:write')")
    public ApiResponse<ProjectDto> createProject(@Valid @RequestBody ProjectBody body) {
        return ApiResponse.ok(projectService.createProject(
                new CreateProjectRequest(body.name(), body.clientName(), body.budgetAmount(), body.startDate(), body.dueDate())
        ));
    }

    @GetMapping("/{projectId}/sprints")
    @PreAuthorize("@authz.hasPermission('projects:read')")
    public ApiResponse<List<SprintDto>> listSprints(@PathVariable UUID projectId) {
        return ApiResponse.ok(projectService.listSprints(projectId));
    }

    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('projects:write')")
    public ApiResponse<SprintDto> createSprint(@Valid @RequestBody SprintBody body) {
        return ApiResponse.ok(projectService.createSprint(
                new CreateSprintRequest(body.projectId(), body.name(), body.goal(), body.startDate(), body.endDate())
        ));
    }

    @GetMapping("/tasks")
    @PreAuthorize("@authz.hasPermission('projects:read')")
    public ApiResponse<List<TaskDto>> listTasks(@RequestParam(required = false) UUID projectId) {
        return ApiResponse.ok(projectService.listTasks(projectId));
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('projects:write')")
    public ApiResponse<TaskDto> createTask(@Valid @RequestBody TaskBody body) {
        return ApiResponse.ok(projectService.createTask(
                new CreateTaskRequest(body.projectId(), body.sprintId(), body.title(), body.taskType(), body.priority(), body.storyPoints(), body.assigneeName(), body.dueDate())
        ));
    }

    @PatchMapping("/tasks/{id}/status")
    @PreAuthorize("@authz.hasPermission('projects:write')")
    public ApiResponse<TaskDto> updateTaskStatus(@PathVariable UUID id, @Valid @RequestBody TaskStatusBody body) {
        return ApiResponse.ok(projectService.updateTaskStatus(id, body.status()));
    }

    @GetMapping("/time-logs")
    @PreAuthorize("@authz.hasPermission('projects:read')")
    public ApiResponse<List<TimeLogDto>> listTimeLogs() {
        return ApiResponse.ok(projectService.listTimeLogs());
    }

    @PostMapping("/time-logs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('projects:write')")
    public ApiResponse<TimeLogDto> logTime(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody TimeLogBody body
    ) {
        return ApiResponse.ok(projectService.logTime(
                principal,
                new LogTimeRequest(body.taskId(), body.minutesLogged(), body.billable(), body.workDate(), body.notes())
        ));
    }

    public record ProjectBody(@NotBlank String name, String clientName, BigDecimal budgetAmount, LocalDate startDate, LocalDate dueDate) {
    }

    public record SprintBody(@NotNull UUID projectId, @NotBlank String name, String goal, LocalDate startDate, LocalDate endDate) {
    }

    public record TaskBody(@NotNull UUID projectId, UUID sprintId, @NotBlank String title, String taskType, String priority, Integer storyPoints, String assigneeName, LocalDate dueDate) {
    }

    public record TaskStatusBody(@NotBlank String status) {
    }

    public record TimeLogBody(@NotNull UUID taskId, @Min(1) int minutesLogged, boolean billable, LocalDate workDate, String notes) {
    }
}

