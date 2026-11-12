package com.zenius.one.modules.ai;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class Phase9AiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void aiAgentsRagHitlWorkflowExecutionAndIsolation() throws Exception {
        JsonNode tenantA = register("AI Officer A", "ai-a@zenius.test", "StrongPass@123", "AI Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("AI Officer B", "ai-b@zenius.test", "StrongPass@123", "AI Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 1. Agents Directory: Global Assistant + 11 Specialized Agents
        mockMvc.perform(get("/api/v1/ai/agents")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)));

        // 2. RAG Knowledge Ingestion + Semantic Search
        mockMvc.perform(post("/api/v1/ai/knowledge")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Enterprise SLA & Escalation Policy",
                                "sourceType", "POLICY",
                                "contentText",
                                "High priority enterprise support tickets require engineering escalation within 15 minutes and executive notification."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkCount", is(1)));

        mockMvc.perform(post("/api/v1/ai/knowledge/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "high priority ticket escalation", "topK", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentTitle", is("Enterprise SLA & Escalation Policy")));

        // 3. Conversational Chat grounded in RAG + HITL Proposal -> Approve creates real Automation Workflow
        MvcResult chatRes = mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "agentCode", "WORKFLOW",
                                "message", "Automate escalation notification when high priority ticket arrives"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.citations[0].documentTitle", is("Enterprise SLA & Escalation Policy")))
                .andExpect(jsonPath("$.data.proposal.status", is("PENDING")))
                .andExpect(jsonPath("$.data.proposal.actionType", is("CREATE_WORKFLOW")))
                .andExpect(jsonPath("$.data.reply.content", containsString("RAG Policy Evidence")))
                .andReturn();

        String convId = objectMapper.readTree(chatRes.getResponse().getContentAsString())
                .get("data").get("conversationId").asText();
        String proposalId = objectMapper.readTree(chatRes.getResponse().getContentAsString())
                .get("data").get("proposal").get("id").asText();

        mockMvc.perform(get("/api/v1/ai/conversations/" + convId)
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(2)));

        mockMvc.perform(post("/api/v1/ai/proposals/" + proposalId + "/approve")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("EXECUTED")))
                .andExpect(jsonPath("$.data.executedAt", notNullValue()));

        // Verify the approved AI proposal created a live workflow rule in Automation
        mockMvc.perform(get("/api/v1/automation/workflows")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].triggerEvent", is("SUPPORT_TICKET_CREATED")));

        // 4. Domain Predictive Insights
        mockMvc.perform(get("/api/v1/ai/insights")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));

        // 5. Tenant Isolation: Org B cannot read Org A's RAG knowledge or proposals
        mockMvc.perform(get("/api/v1/ai/knowledge")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/ai/proposals")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}

