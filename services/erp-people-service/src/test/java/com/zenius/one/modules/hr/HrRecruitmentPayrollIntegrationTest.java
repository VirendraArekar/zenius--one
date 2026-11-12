package com.zenius.one.modules.hr;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
class HrRecruitmentPayrollIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void recruitHireAttendanceLeaveAndPayrollWorkflowWithIsolation() throws Exception {
        JsonNode tenantA = register("HR Dir A", "hr-a@zenius.test", "StrongPass@123", "People Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("HR Dir B", "hr-b@zenius.test", "StrongPass@123", "People Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 1. Recruitment: Job -> Candidate -> Hire into HR Employee
        MvcResult jobRes = mockMvc.perform(post("/api/v1/recruitment/jobs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Staff Java Engineer",
                                "department", "Engineering",
                                "location", "Bengaluru"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.jobCode", is("JOB-0001")))
                .andReturn();
        String jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult candRes = mockMvc.perform(post("/api/v1/recruitment/candidates")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobId", jobId,
                                "fullName", "Ananya Rao",
                                "email", "ananya@dev.io",
                                "skills", "Java 21, Spring Boot, React",
                                "experienceYears", 6
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String candId = objectMapper.readTree(candRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult hireRes = mockMvc.perform(post("/api/v1/recruitment/candidates/" + candId + "/hire")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("baseMonthlySalary", 100000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode", is("EMP-0001")))
                .andReturn();
        String empId = objectMapper.readTree(hireRes.getResponse().getContentAsString()).get("data").get("employeeId").asText();

        // 2. HR Attendance Check-In / Check-Out & Leave Approval
        mockMvc.perform(post("/api/v1/hr/attendance/check-in")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "employeeId", empId,
                                "workMode", "REMOTE",
                                "latitude", 12.9716,
                                "longitude", 77.5946
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PRESENT")));

        mockMvc.perform(post("/api/v1/hr/attendance/" + empId + "/check-out")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkOutAt").isNotEmpty());

        MvcResult leaveRes = mockMvc.perform(post("/api/v1/hr/leaves")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "employeeId", empId,
                                "leaveType", "ANNUAL",
                                "startDate", LocalDate.now().plusDays(5).toString(),
                                "endDate", LocalDate.now().plusDays(6).toString(),
                                "reason", "Family trip"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.daysCount", is(2)))
                .andReturn();
        String leaveId = objectMapper.readTree(leaveRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/hr/leaves/" + leaveId + "/decision")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // 3. Payroll Run (Gross 100,000 -> Basic 50,000, PF 6,000, TDS 10,000, Net 84,000)
        mockMvc.perform(post("/api/v1/payroll/runs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("payMonth", "2026-11"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeesCount", is(1)))
                .andExpect(jsonPath("$.data.totalGross", is(100000.0)))
                .andExpect(jsonPath("$.data.totalDeductions", is(16000.0)))
                .andExpect(jsonPath("$.data.totalNetPay", is(84000.0)));

        // Duplicate payroll run for same month rejected
        mockMvc.perform(post("/api/v1/payroll/runs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("payMonth", "2026-11"))))
                .andExpect(status().isConflict());

        // 4. Tenant Isolation: Org B sees zero employees & cannot access Org A payroll
        mockMvc.perform(get("/api/v1/hr/employees")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/payroll/runs")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}

