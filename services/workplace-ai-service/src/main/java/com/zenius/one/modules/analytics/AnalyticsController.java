package com.zenius.one.modules.analytics;

import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & BI", description = "Cross-app BI executive metrics, custom dashboards, reports, and CSV exports")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/pulse")
    @PreAuthorize("@authz.hasPermission('analytics:read')")
    public ApiResponse<AnalyticsService.ExecutivePulse> getPulse() {
        return ApiResponse.ok(analyticsService.getExecutivePulse());
    }

    @GetMapping("/dashboards")
    @PreAuthorize("@authz.hasPermission('analytics:read')")
    public ApiResponse<List<AnalyticsService.DashboardSummary>> listDashboards() {
        return ApiResponse.ok(analyticsService.listDashboards());
    }

    @PostMapping("/dashboards")
    @PreAuthorize("@authz.hasPermission('analytics:write')")
    public ApiResponse<AnalyticsService.DashboardSummary> createDashboard(
            @Valid @RequestBody AnalyticsService.CreateDashboardRequest request) {
        return ApiResponse.ok(analyticsService.createDashboard(request));
    }

    @GetMapping("/reports")
    @PreAuthorize("@authz.hasPermission('analytics:read')")
    public ApiResponse<List<AnalyticsService.ReportSummary>> listReports() {
        return ApiResponse.ok(analyticsService.listReports());
    }

    @PostMapping("/reports")
    @PreAuthorize("@authz.hasPermission('analytics:write')")
    public ApiResponse<AnalyticsService.ReportSummary> createReport(
            @Valid @RequestBody AnalyticsService.CreateReportRequest request) {
        return ApiResponse.ok(analyticsService.createReport(request));
    }

    @PostMapping("/reports/{id}/run")
    @PreAuthorize("@authz.hasPermission('analytics:read')")
    public ApiResponse<AnalyticsService.ReportExecutionResult> runReport(@PathVariable UUID id) {
        return ApiResponse.ok(analyticsService.runReport(id));
    }

    @GetMapping("/reports/{id}/export")
    @PreAuthorize("@authz.hasPermission('analytics:read')")
    public ResponseEntity<String> exportReportCsv(@PathVariable UUID id) {
        String csv = analyticsService.exportReportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + ".csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}

