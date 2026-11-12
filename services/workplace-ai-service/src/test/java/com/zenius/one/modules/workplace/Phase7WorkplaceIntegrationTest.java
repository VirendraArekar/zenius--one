package com.zenius.one.modules.workplace;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class Phase7WorkplaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void projectsCollabDocumentsSignaturesAndLegalEndToEndWithIsolation() throws Exception {
        JsonNode tenantA = register("Workplace Admin A", "wp-a@zenius.test", "StrongPass@123", "Workplace Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("Workplace Admin B", "wp-b@zenius.test", "StrongPass@123", "Workplace Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 1. Projects: Project -> Sprint -> Task -> Status update -> Time Log
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Zenius Cloud Rollout",
                                "clientName", "Acme Corp",
                                "budgetAmount", 45000,
                                "startDate", LocalDate.now().toString(),
                                "dueDate", LocalDate.now().plusMonths(3).toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.projectCode", is("PRJ-001")))
                .andReturn();
        String projectId = objectMapper.readTree(projRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult sprintRes = mockMvc.perform(post("/api/v1/projects/sprints")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "projectId", projectId,
                                "name", "Sprint 1",
                                "goal", "Core Auth & SSO"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", is("Sprint 1")))
                .andReturn();
        String sprintId = objectMapper.readTree(sprintRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult taskRes = mockMvc.perform(post("/api/v1/projects/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "projectId", projectId,
                                "sprintId", sprintId,
                                "title", "Configure SAML IdP",
                                "taskType", "STORY",
                                "priority", "HIGH",
                                "storyPoints", 5,
                                "assigneeName", "Riya Kapoor"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("TODO")))
                .andReturn();
        String taskId = objectMapper.readTree(taskRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(patch("/api/v1/projects/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));

        mockMvc.perform(post("/api/v1/projects/time-logs")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "taskId", taskId,
                                "minutesLogged", 90,
                                "billable", true,
                                "notes", "Implemented metadata parser"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.minutesLogged", is(90)));

        mockMvc.perform(get("/api/v1/projects/tasks")
                        .param("projectId", projectId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].loggedMinutes", is(90)));

        // 2. Collaboration: Mail, Channels + Messages, Meetings
        mockMvc.perform(post("/api/v1/collab/mail")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientEmail", "client@acme.example",
                                "subject", "Kickoff Deck",
                                "body", "Attached the rollout timeline.",
                                "labels", "IMPORTANT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.folder", is("SENT")));

        mockMvc.perform(get("/api/v1/collab/mail")
                        .param("folder", "INBOX")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        MvcResult chanRes = mockMvc.perform(post("/api/v1/collab/channels")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "cloud-rollout",
                                "description", "Delivery team room",
                                "privateChannel", false))))
                .andExpect(status().isCreated())
                .andReturn();
        String channelId = objectMapper.readTree(chanRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/collab/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Sprint 1 kicked off!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body", is("Sprint 1 kicked off!")));

        mockMvc.perform(post("/api/v1/collab/meetings")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Weekly Architecture Sync",
                                "scheduledAt", Instant.now().plusSeconds(3600).toString(),
                                "durationMinutes", 45))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.meetingLink", notNullValue()));

        // 3. WorkDrive & Office: Folder -> Create Doc -> Edit (v2) -> Signature Request & Sign
        MvcResult folderRes = mockMvc.perform(post("/api/v1/docs/folders")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Legal & MSA",
                                "shared", true))))
                .andExpect(status().isOk())
                .andReturn();
        String folderId = objectMapper.readTree(folderRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult docRes = mockMvc.perform(post("/api/v1/docs/files")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "folderId", folderId,
                                "title", "Master Services Agreement",
                                "docType", "WORD",
                                "contentBody", "Section 1: Scope of Services"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file.currentVersion", is(1)))
                .andExpect(jsonPath("$.data.revisions", hasSize(1)))
                .andReturn();
        String docId = objectMapper.readTree(docRes.getResponse().getContentAsString())
                .get("data").get("file").get("id").asText();

        mockMvc.perform(patch("/api/v1/docs/files/" + docId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Master Services Agreement (Final)",
                                "contentBody", "Section 1: Scope & SLA 99.95%"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file.currentVersion", is(2)))
                .andExpect(jsonPath("$.data.revisions", hasSize(2)));

        MvcResult sigReqRes = mockMvc.perform(post("/api/v1/docs/files/" + docId + "/signature-requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "signerName", "Acme General Counsel",
                                "signerEmail", "counsel@acme.example"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn();
        String sigId = objectMapper.readTree(sigReqRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/docs/signatures/" + sigId + "/sign")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SIGNED")))
                .andExpect(jsonPath("$.data.signatureHash", notNullValue()));

        // 4. Legal CLM: Create -> Approve -> Execute
        MvcResult ctrRes = mockMvc.perform(post("/api/v1/legal/contracts")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Acme FY27 MSA",
                                "counterpartyName", "Acme Corp",
                                "contractType", "MSA",
                                "valueAmount", 120000,
                                "effectiveDate", LocalDate.now().toString(),
                                "expirationDate", LocalDate.now().plusYears(1).toString(),
                                "documentId", docId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractNumber", is("CTR-00001")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn();
        String ctrId = objectMapper.readTree(ctrRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(patch("/api/v1/legal/contracts/" + ctrId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        mockMvc.perform(patch("/api/v1/legal/contracts/" + ctrId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "EXECUTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("EXECUTED")));

        // 5. Tenant Isolation
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/legal/contracts")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/docs/files")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}
