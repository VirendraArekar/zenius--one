package com.zenius.one.modules.marketing;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class MarketingAndSupportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void marketingAutomationAndSupportServiceFlowsAndIsolation() throws Exception {
        JsonNode tenantA = register("Ops A", "ops-a@zenius.test", "StrongPass@123", "Ops Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("Ops B", "ops-b@zenius.test", "StrongPass@123", "Ops Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 1. Marketing: Subscriber + Segment + Campaign Launch
        mockMvc.perform(post("/api/v1/marketing/subscribers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "vip-buyer@client.io",
                                "fullName", "VIP Buyer",
                                "tags", "enterprise,vip",
                                "source", "WEBINAR"
                        ))))
                .andExpect(status().isCreated());

        MvcResult segRes = mockMvc.perform(post("/api/v1/marketing/segments")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Enterprise VIPs",
                                "filterTag", "vip"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.matchingSubscribers", is(1)))
                .andReturn();
        String segmentId = objectMapper.readTree(segRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult campRes = mockMvc.perform(post("/api/v1/marketing/campaigns")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "segmentId", segmentId,
                                "name", "Q4 Enterprise Launch",
                                "subject", "Introducing Zenius One",
                                "bodyContent", "Welcome to Zenius One."
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String campaignId = objectMapper.readTree(campRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/marketing/campaigns/" + campaignId + "/launch")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.sentCount", is(1)));

        // 2. Marketing Form -> Auto-creates CRM Lead
        MvcResult formRes = mockMvc.perform(post("/api/v1/marketing/forms")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Demo Request",
                                "title", "Book a Zenius One Demo",
                                "autoCreateLead", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String formId = objectMapper.readTree(formRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/marketing/forms/" + formId + "/submit")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Inbound Prospect",
                                "email", "inbound@prospect.io",
                                "companyName", "Prospect Corp"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdLeadId").isNotEmpty());

        mockMvc.perform(get("/api/v1/crm/leads")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(post("/api/v1/marketing/coupons")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "SAVE20",
                                "discountPercent", new BigDecimal("20.00")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", is("SAVE20")));

        // 3. Support: SLA + Ticket + Comment + Resolve + CSAT + KB + Field Service
        mockMvc.perform(get("/api/v1/support/slas")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));

        MvcResult ticketRes = mockMvc.perform(post("/api/v1/support/tickets")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "SSO SAML setup inquiry",
                                "description", "Need help configuring enterprise SSO.",
                                "channel", "EMAIL",
                                "priority", "HIGH",
                                "requesterName", "Rohan Verma",
                                "requesterEmail", "rohan@client.io"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ticketNumber", is("TKT-00001")))
                .andExpect(jsonPath("$.data.slaDueAt").isNotEmpty())
                .andReturn();
        String ticketId = objectMapper.readTree(ticketRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/support/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "We have reviewed your metadata configuration.",
                                "internalNote", false
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/support/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED", "escalate", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));

        mockMvc.perform(post("/api/v1/support/tickets/" + ticketId + "/csat")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", 5, "feedback", "Fast resolution!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.csatRating", is(5)));

        mockMvc.perform(post("/api/v1/support/kb")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Configuring SSO & MFA",
                                "category", "Security",
                                "content", "Step-by-step enterprise SSO guide.",
                                "published", true
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/support/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ticketId", ticketId,
                                "customerName", "Rohan Verma",
                                "serviceAddress", "MG Road, Bengaluru"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.appointmentNumber", is("FSV-00001")));

        mockMvc.perform(get("/api/v1/support/dashboard")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolvedTickets", is(1)))
                .andExpect(jsonPath("$.data.averageCsat", is(5.0)))
                .andExpect(jsonPath("$.data.fieldAppointments", is(1)));

        // 4. Tenant Isolation: Org B sees zero tickets & cannot cross into Org A
        mockMvc.perform(get("/api/v1/support/tickets")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/marketing/campaigns")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}

