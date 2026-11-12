package com.zenius.one.modules.analytics;

import com.zenius.one.modules.projects.ProjTaskEntity;
import com.zenius.one.modules.projects.ProjTaskRepository;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final Set<String> VALID_SOURCES =
            Set.of("CRM_DEALS", "FINANCE_INVOICES", "SUPPORT_TICKETS", "HR_EMPLOYEES", "PROJECTS_TASKS");
    private static final Set<String> VALID_CHARTS = Set.of("BAR", "LINE", "PIE", "KPI", "TABLE", "PIVOT");

    private final BiDashboardRepository dashboardRepository;
    private final BiReportRepository reportRepository;
    private final ProjTaskRepository taskRepository;
    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(
            BiDashboardRepository dashboardRepository,
            BiReportRepository reportRepository,
            ProjTaskRepository taskRepository,
            JdbcTemplate jdbcTemplate) {
        this.dashboardRepository = dashboardRepository;
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public ExecutivePulse getExecutivePulse() {
        UUID orgId = TenantContext.requireOrganizationId();

        long leadCount = queryCount("SELECT COUNT(*) FROM crm_leads WHERE organization_id = ? AND deleted_at IS NULL", orgId);
        Map<String, Object> dealStats = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt, COALESCE(SUM(expected_revenue), 0) AS exp FROM crm_deals WHERE organization_id = ? AND deleted_at IS NULL",
                orgId);
        long dealCount = toLong(dealStats.get("cnt"));
        BigDecimal pipelineValue = toDecimal(dealStats.get("amt"));
        BigDecimal weightedForecast = toDecimal(dealStats.get("exp"));

        Map<String, Object> invStats = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS tot, COALESCE(SUM(balance_due), 0) AS due FROM fin_invoices WHERE organization_id = ?",
                orgId);
        long invoiceCount = toLong(invStats.get("cnt"));
        BigDecimal totalInvoiced = toDecimal(invStats.get("tot"));
        BigDecimal totalReceivable = toDecimal(invStats.get("due"));

        long totalTickets = queryCount("SELECT COUNT(*) FROM sup_tickets WHERE organization_id = ?", orgId);
        long openTickets = queryCount(
                "SELECT COUNT(*) FROM sup_tickets WHERE organization_id = ? AND UPPER(status) NOT IN ('CLOSED', 'RESOLVED')",
                orgId);
        long headcount = queryCount("SELECT COUNT(*) FROM hr_employees WHERE organization_id = ?", orgId);

        List<ProjTaskEntity> tasks = taskRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        long completedTasks = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();

        List<ReportDataRow> moduleBreakdown = List.of(
                new ReportDataRow("CRM Leads", leadCount, BigDecimal.ZERO),
                new ReportDataRow("CRM Deals", dealCount, pipelineValue),
                new ReportDataRow("Invoices", invoiceCount, totalInvoiced),
                new ReportDataRow("Support Tickets", totalTickets, BigDecimal.ZERO),
                new ReportDataRow("Employees", headcount, BigDecimal.ZERO),
                new ReportDataRow("Project Tasks", tasks.size(), BigDecimal.ZERO));

        return new ExecutivePulse(
                leadCount,
                dealCount,
                pipelineValue,
                weightedForecast,
                totalInvoiced,
                totalReceivable,
                openTickets,
                headcount,
                tasks.size(),
                completedTasks,
                moduleBreakdown);
    }

    @Transactional(readOnly = true)
    public List<DashboardSummary> listDashboards() {
        UUID orgId = TenantContext.requireOrganizationId();
        return dashboardRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(DashboardSummary::from)
                .toList();
    }

    @Transactional
    public DashboardSummary createDashboard(@Valid CreateDashboardRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        BiDashboardEntity dash = new BiDashboardEntity();
        dash.setOrganizationId(orgId);
        dash.setName(request.name().trim());
        dash.setDescription(request.description() == null ? null : request.description().trim());
        dash.setShared(request.shared() == null || request.shared());
        dash.setLayoutJson(
                request.layoutJson() == null || request.layoutJson().isBlank() ? "[]" : request.layoutJson());
        return DashboardSummary.from(dashboardRepository.save(dash));
    }

    @Transactional(readOnly = true)
    public List<ReportSummary> listReports() {
        UUID orgId = TenantContext.requireOrganizationId();
        return reportRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(ReportSummary::from)
                .toList();
    }

    @Transactional
    public ReportSummary createReport(@Valid CreateReportRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        String source = request.dataSource().trim().toUpperCase();
        if (!VALID_SOURCES.contains(source)) {
            throw new IllegalArgumentException("Unsupported dataSource: " + request.dataSource());
        }
        String chartType = request.chartType() == null || request.chartType().isBlank()
                ? "BAR"
                : request.chartType().trim().toUpperCase();
        if (!VALID_CHARTS.contains(chartType)) {
            throw new IllegalArgumentException("Unsupported chartType: " + chartType);
        }
        if (request.dashboardId() != null) {
            dashboardRepository
                    .findByIdAndOrganizationId(request.dashboardId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dashboard not found"));
        }

        BiReportEntity report = new BiReportEntity();
        report.setOrganizationId(orgId);
        report.setDashboardId(request.dashboardId());
        report.setName(request.name().trim());
        report.setDataSource(source);
        report.setChartType(chartType);
        report.setMetric(
                request.metric() == null || request.metric().isBlank()
                        ? "COUNT"
                        : request.metric().trim().toUpperCase());
        report.setDimension(
                request.dimension() == null || request.dimension().isBlank()
                        ? "STATUS"
                        : request.dimension().trim().toUpperCase());
        report.setConfigJson(
                request.configJson() == null || request.configJson().isBlank() ? "{}" : request.configJson());
        return ReportSummary.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public ReportExecutionResult runReport(UUID reportId) {
        UUID orgId = TenantContext.requireOrganizationId();
        BiReportEntity report = reportRepository
                .findByIdAndOrganizationId(reportId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        List<ReportDataRow> rows = aggregateSource(orgId, report.getDataSource());
        return new ReportExecutionResult(ReportSummary.from(report), rows, Instant.now());
    }

    @Transactional(readOnly = true)
    public String exportReportCsv(UUID reportId) {
        ReportExecutionResult result = runReport(reportId);
        StringBuilder sb = new StringBuilder("label,count,amount\n");
        for (ReportDataRow row : result.rows()) {
            sb.append('"')
                    .append(row.label().replace("\"", "\"\""))
                    .append('"')
                    .append(',')
                    .append(row.count())
                    .append(',')
                    .append(row.amount())
                    .append('\n');
        }
        return sb.toString();
    }

    private List<ReportDataRow> aggregateSource(UUID orgId, String dataSource) {
        return switch (dataSource) {
            case "FINANCE_INVOICES" -> jdbcTemplate.query(
                    "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amt FROM fin_invoices WHERE organization_id = ? GROUP BY status",
                    (rs, rowNum) -> new ReportDataRow(rs.getString("status"), rs.getLong("cnt"), rs.getBigDecimal("amt")),
                    orgId);
            case "SUPPORT_TICKETS" -> jdbcTemplate.query(
                    "SELECT priority, COUNT(*) AS cnt FROM sup_tickets WHERE organization_id = ? GROUP BY priority",
                    (rs, rowNum) -> new ReportDataRow(rs.getString("priority"), rs.getLong("cnt"), BigDecimal.ZERO),
                    orgId);
            case "PROJECTS_TASKS" -> {
                Map<String, ReportDataRow> grouped = new LinkedHashMap<>();
                for (ProjTaskEntity task : taskRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)) {
                    String key = task.getStatus();
                    ReportDataRow cur = grouped.getOrDefault(key, new ReportDataRow(key, 0, BigDecimal.ZERO));
                    grouped.put(key, new ReportDataRow(key, cur.count() + 1, BigDecimal.ZERO));
                }
                yield new ArrayList<>(grouped.values());
            }
            case "HR_EMPLOYEES" -> jdbcTemplate.query(
                    "SELECT status, COUNT(*) AS cnt FROM hr_employees WHERE organization_id = ? GROUP BY status",
                    (rs, rowNum) -> new ReportDataRow(rs.getString("status"), rs.getLong("cnt"), BigDecimal.ZERO),
                    orgId);
            default -> {
                Map<String, Object> stats = jdbcTemplate.queryForMap(
                        "SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt FROM crm_deals WHERE organization_id = ? AND deleted_at IS NULL",
                        orgId);
                yield List.of(new ReportDataRow("ACTIVE_PIPELINE", toLong(stats.get("cnt")), toDecimal(stats.get("amt"))));
            }
        };
    }

    private long queryCount(String sql, UUID orgId) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class, orgId);
        return val == null ? 0L : val;
    }

    private long toLong(Object val) {
        return val instanceof Number n ? n.longValue() : 0L;
    }

    private BigDecimal toDecimal(Object val) {
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        return val == null ? BigDecimal.ZERO : new BigDecimal(val.toString());
    }

    public record CreateDashboardRequest(
            @NotBlank @Size(max = 160) String name, @Size(max = 500) String description, Boolean shared, String layoutJson) {}

    public record CreateReportRequest(
            UUID dashboardId,
            @NotBlank @Size(max = 160) String name,
            @NotBlank String dataSource,
            String chartType,
            String metric,
            String dimension,
            String configJson) {}

    public record DashboardSummary(
            UUID id, String name, String description, boolean shared, String layoutJson, Instant createdAt) {
        public static DashboardSummary from(BiDashboardEntity entity) {
            return new DashboardSummary(
                    entity.getId(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.isShared(),
                    entity.getLayoutJson(),
                    entity.getCreatedAt());
        }
    }

    public record ReportSummary(
            UUID id,
            UUID dashboardId,
            String name,
            String dataSource,
            String chartType,
            String metric,
            String dimension,
            Instant createdAt) {
        public static ReportSummary from(BiReportEntity entity) {
            return new ReportSummary(
                    entity.getId(),
                    entity.getDashboardId(),
                    entity.getName(),
                    entity.getDataSource(),
                    entity.getChartType(),
                    entity.getMetric(),
                    entity.getDimension(),
                    entity.getCreatedAt());
        }
    }

    public record ReportDataRow(String label, long count, BigDecimal amount) {}

    public record ReportExecutionResult(ReportSummary report, List<ReportDataRow> rows, Instant executedAt) {}

    public record ExecutivePulse(
            long leadCount,
            long dealCount,
            BigDecimal pipelineValue,
            BigDecimal weightedForecast,
            BigDecimal totalInvoiced,
            BigDecimal totalReceivable,
            long openTickets,
            long employeeHeadcount,
            long totalTasks,
            long completedTasks,
            List<ReportDataRow> moduleBreakdown) {}
}
