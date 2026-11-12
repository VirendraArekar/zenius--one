package com.zenius.one.modules.itsecurity;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class Phase10SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void securityHeadersAssetsIncidentsPolicyAndCrossTenantHardening() throws Exception {
        // 1. Verify Hardened Security Response Headers & Unauthenticated Denial
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Content-Security-Policy"));

        mockMvc.perform(get("/api/v1/security/assets"))
                .andExpect(status().isForbidden());

        JsonNode tenantA = register("CISO A", "ciso-a@zenius.test", "StrongPass@123", "Sec Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("CISO B", "ciso-b@zenius.test", "StrongPass@123", "Sec Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 2. Register IT Asset
        mockMvc.perform(post("/api/v1/security/assets")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Prod-K8s-Ingress-01",
                                "assetType", "SERVER",
                                "ownerName", "Platform SRE",
                                "osVersion", "Ubuntu 24.04 LTS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetTag", is("AST-0001")))
                .andExpect(jsonPath("$.data.complianceState", is("COMPLIANT")));

        // 3. Open & Resolve Security Incident
        MvcResult incRes = mockMvc.perform(post("/api/v1/security/incidents")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Suspicious API burst blocked",
                                "severity", "HIGH"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentNumber", is("INC-0001")))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andReturn();
        String incId = objectMapper.readTree(incRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/security/incidents/" + incId + "/resolve")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resolutionNotes", "Revoked compromised API key and enforced CIDR allowlist."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.resolvedAt", notNullValue()));

        // 4. Org Security Policy Update
        mockMvc.perform(put("/api/v1/security/policy")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "minPasswordLength", 14,
                                "requireMfa", true,
                                "sessionTimeoutMinutes", 120,
                                "allowedIpCidr", "10.0.0.0/8"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requireMfa", is(true)))
                .andExpect(jsonPath("$.data.minPasswordLength", is(14)));

        // 5. Cross-Tenant Isolation
        mockMvc.perform(get("/api/v1/security/assets")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/security/incidents")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "password", password,
                                "organizationName", orgName))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}

