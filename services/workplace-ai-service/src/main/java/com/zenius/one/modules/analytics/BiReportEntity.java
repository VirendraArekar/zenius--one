package com.zenius.one.modules.analytics;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "bi_reports")
public class BiReportEntity extends TenantScopedEntity {

    @Column(name = "dashboard_id")
    private UUID dashboardId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "data_source", nullable = false, length = 48)
    private String dataSource;

    @Column(name = "chart_type", nullable = false, length = 32)
    private String chartType = "BAR";

    @Column(nullable = false, length = 64)
    private String metric = "COUNT";

    @Column(nullable = false, length = 64)
    private String dimension = "STATUS";

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson = "{}";

    public UUID getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(UUID dashboardId) {
        this.dashboardId = dashboardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}

