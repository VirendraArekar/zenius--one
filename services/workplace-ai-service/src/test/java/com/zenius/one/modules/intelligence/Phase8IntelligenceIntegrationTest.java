package com.zenius.one.modules.intelligence;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase8IntelligenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void analyticsCreatorAndAutomationWithTenantIsolation() throws Exception {
        JsonNode tenantA = register("BI Admin A", "bi-a@zenius.test", "StrongPass@123", "Intel Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("BI Admin B", "bi-b@zenius.test", "StrongPass@123", "Intel Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // Seed real domain data (Project & Task) for live BI aggregation
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "BI Ops Rollout"))))
                .andExpect(status().isCreated())
                .andReturn();
        String projId = objectMapper.readTree(projRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/projects/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "projectId", projId,
                                "title", "Verify telemetry pipeline",
                                "priority", "HIGH"))))
                .andExpect(status().isCreated());

        // 1. Analytics & BI: Live Pulse, Dashboard, Report, Run & CSV Export
        mockMvc.perform(get("/api/v1/analytics/pulse")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTasks", is(1)));

        MvcResult dashRes = mockMvc.perform(post("/api/v1/analytics/dashboards")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Executive Ops BI",
                                "description", "Cross-app live KPIs"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Executive Ops BI")))
                .andReturn();
        String dashId = objectMapper.readTree(dashRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult reportRes = mockMvc.perform(post("/api/v1/analytics/reports")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dashboardId", dashId,
                                "name", "Tasks by Status",
                                "dataSource", "PROJECTS_TASKS",
                                "chartType", "BAR"))))
                .andExpect(status().isOk())
                .andReturn();
        String reportId = objectMapper.readTree(reportRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/analytics/reports/" + reportId + "/run")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].label", is("TODO")))
                .andExpect(jsonPath("$.data.rows[0].count", is(1)));

        mockMvc.perform(get("/api/v1/analytics/reports/" + reportId + "/export")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("label,count,amount")))
                .andExpect(content().string(containsString("\"TODO\",1")));

        // 2. Low-Code Creator: App -> Form with Formula -> Submit Record -> Publish
        MvcResult appRes = mockMvc.perform(post("/api/v1/creator/apps")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vendor Expense Tracker",
                                "description", "Custom PO intake"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appCode", is("APP-001")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn();
        String appId = objectMapper.readTree(appRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult formRes = mockMvc.perform(post("/api/v1/creator/forms")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "appId", appId,
                                "name", "Line Item Entry",
                                "fields", List.of(
                                        Map.of("key", "item", "label", "Item", "type", "TEXT", "required", true),
                                        Map.of("key", "qty", "label", "Quantity", "type", "NUMBER", "required", true),
                                        Map.of("key", "unitPrice", "label", "Unit Price", "type", "NUMBER", "required", true),
                                        Map.of("key", "total", "label", "Total", "type", "FORMULA", "required", false, "formula", "qty * unitPrice"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formCode", is("FRM-001")))
                .andReturn();
        String formId = objectMapper.readTree(formRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/creator/forms/" + formId + "/records")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("item", "Cloud Nodes", "qty", 5, "unitPrice", 100)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordNumber", is("REC-0001")))
                .andExpect(jsonPath("$.data.data.total", is(500.0)));

        mockMvc.perform(post("/api/v1/creator/apps/" + appId + "/publish")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.data.appVersion", is(2)));

        // 3. Workflow Automation: Condition check -> Trigger -> Notification + Execution log
        MvcResult wfRes = mockMvc.perform(post("/api/v1/automation/workflows")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "High-Value Deal Escalation",
                                "triggerEvent", "CRM_DEAL_WON",
                                "conditionExpr", "amount>1000",
                                "actionType", "CREATE_NOTIFICATION"))))
                .andExpect(status().isOk())
                .andReturn();
        String wfId = objectMapper.readTree(wfRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/automation/workflows/" + wfId + "/trigger")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SKIPPED")));

        mockMvc.perform(post("/api/v1/automation/workflows/" + wfId + "/trigger")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 2500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCESS")));

        mockMvc.perform(patch("/api/v1/automation/workflows/" + wfId + "/toggle")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled", is(false)))
                .andExpect(jsonPath("$.data.runCount", is(1)));

        mockMvc.perform(get("/api/v1/automation/executions")
                        .param("workflowId", wfId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 4. Tenant Isolation
        mockMvc.perform(get("/api/v1/analytics/dashboards")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/creator/apps")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}

