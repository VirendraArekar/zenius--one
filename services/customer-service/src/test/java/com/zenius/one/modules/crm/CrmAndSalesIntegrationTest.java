package com.zenius.one.modules.crm;

import static org.assertj.core.api.Assertions.assertThat;
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
class CrmAndSalesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void leadToCashWorkflowAndTenantIsolation() throws Exception {
        JsonNode tenantA = register("CRM Owner A", "crm-a@zenius.test", "PassWord@123", "CRM Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("CRM Owner B", "crm-b@zenius.test", "PassWord@123", "CRM Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 1. Create Lead in Org A
        MvcResult leadRes = mockMvc.perform(post("/api/v1/crm/leads")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Priya Sharma",
                                "email", "priya@globaltech.io",
                                "phone", "+91-9876543210",
                                "companyName", "GlobalTech Systems",
                                "source", "REFERRAL",
                                "estimatedValue", 50000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("NEW")))
                .andReturn();
        JsonNode lead = objectMapper.readTree(leadRes.getResponse().getContentAsString()).get("data");
        String leadId = lead.get("id").asText();
        assertThat(lead.get("leadScore").asInt()).isGreaterThanOrEqualTo(75);

        // Duplicate email check
        mockMvc.perform(post("/api/v1/crm/leads")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Priya Duplicate",
                                "email", "priya@globaltech.io"
                        ))))
                .andExpect(status().isConflict());

        // 2. Convert Lead -> Account + Contact + Deal
        MvcResult convRes = mockMvc.perform(post("/api/v1/crm/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "createDeal", true,
                                "dealTitle", "GlobalTech Enterprise Rollout",
                                "dealAmount", 50000
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").isNotEmpty())
                .andExpect(jsonPath("$.data.contactId").isNotEmpty())
                .andExpect(jsonPath("$.data.dealId").isNotEmpty())
                .andReturn();
        JsonNode conv = objectMapper.readTree(convRes.getResponse().getContentAsString()).get("data");
        String accountId = conv.get("accountId").asText();
        String contactId = conv.get("contactId").asText();
        String dealId = conv.get("dealId").asText();

        // 3. Fetch pipeline stages and advance Deal to Proposal & Quote (55%)
        MvcResult pipeRes = mockMvc.perform(get("/api/v1/crm/pipelines")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stages = objectMapper.readTree(pipeRes.getResponse().getContentAsString())
                .get("data").get(0).get("stages");
        String proposalStageId = stages.get(2).get("id").asText();

        mockMvc.perform(patch("/api/v1/crm/deals/" + dealId + "/stage")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("stageId", proposalStageId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.probability", is(55)))
                .andExpect(jsonPath("$.data.expectedRevenue", is(27500.0)));

        // 4. Create Product -> Quote -> Convert Quote to Sales Order
        MvcResult prodRes = mockMvc.perform(post("/api/v1/crm/products")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", "Z1-ENT-SEAT",
                                "name", "Zenius One Enterprise License",
                                "category", "SAAS",
                                "unitPrice", new BigDecimal("1000.00"),
                                "taxRate", new BigDecimal("18.00")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String productId = objectMapper.readTree(prodRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult quoteRes = mockMvc.perform(post("/api/v1/crm/quotes")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "accountId", accountId,
                                "contactId", contactId,
                                "dealId", dealId,
                                "subject", "Annual 10-Seat License Quote",
                                "lines", List.of(Map.of(
                                        "productId", productId,
                                        "itemName", "Zenius One Enterprise License",
                                        "quantity", 10,
                                        "unitPrice", 1000,
                                        "discountPercent", 10,
                                        "taxRate", 18
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal", is(9000.0)))
                .andExpect(jsonPath("$.data.taxTotal", is(1620.0)))
                .andExpect(jsonPath("$.data.grandTotal", is(10620.0)))
                .andReturn();
        String quoteId = objectMapper.readTree(quoteRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/crm/quotes/" + quoteId + "/convert-to-order")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber", is("SO-00001")))
                .andExpect(jsonPath("$.data.grandTotal", is(10620.0)));

        // 5. Verify Dashboard aggregates
        mockMvc.perform(get("/api/v1/crm/dashboard")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalLeads", is(1)))
                .andExpect(jsonPath("$.data.convertedLeads", is(1)))
                .andExpect(jsonPath("$.data.openDeals", is(1)))
                .andExpect(jsonPath("$.data.weightedForecast", is(27500.0)));

        // 6. Verify Org B cannot see Org A's CRM records
        mockMvc.perform(get("/api/v1/crm/leads")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/crm/leads")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}

