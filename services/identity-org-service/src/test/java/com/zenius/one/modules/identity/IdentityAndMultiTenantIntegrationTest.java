package com.zenius.one.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenius.one.modules.identity.security.TotpService;
import java.time.Instant;
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
class IdentityAndMultiTenantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TotpService totpService;

    @Test
    void enforcesMultiTenantIsolationRbacRotationMfaAndApiKeys() throws Exception {
        // 1. Register Tenant A Owner
        JsonNode tenantA = registerUser("Alice Owner", "alice@orga.test", "StrongPass@123", "Org Alpha");
        String aliceToken = tenantA.get("accessToken").asText();
        String aliceRefresh = tenantA.get("refreshToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        // 2. Register Tenant B Owner
        JsonNode tenantB = registerUser("Bob Owner", "bob@orgb.test", "StrongPass@123", "Org Beta");
        String bobToken = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 3. Alice creates a branch in Org A
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "BLR-01",
                                "name", "Bengaluru Hub",
                                "city", "Bengaluru",
                                "country", "India",
                                "headquarters", false
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", is("BLR-01")));

        // 4. Tenant isolation: Bob cannot access Org A with X-Organization-Id header
        mockMvc.perform(get("/api/v1/branches")
                        .header("Authorization", "Bearer " + bobToken)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());

        // 5. Bob only sees Org B's default HQ branch (size 1), never BLR-01
        mockMvc.perform(get("/api/v1/branches")
                        .header("Authorization", "Bearer " + bobToken)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code", is("HQ")));

        // 6. RBAC: Alice adds a standard MEMBER user who lacks branch:write permission
        mockMvc.perform(post("/api/v1/members")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Charlie Member",
                                "email", "charlie@orga.test",
                                "temporaryPassword", "Charlie@123",
                                "designation", "Analyst"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("charlie@orga.test")));

        String charlieToken = loginAndGetToken("charlie@orga.test", "Charlie@123");

        // Charlie can read branches (branch:read)
        mockMvc.perform(get("/api/v1/branches")
                        .header("Authorization", "Bearer " + charlieToken)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Charlie is denied creating a branch (lacks branch:write)
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + charlieToken)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "NYC-01",
                                "name", "NYC Office"
                        ))))
                .andExpect(status().isForbidden());

        // 7. Refresh token rotation & reuse detection
        MvcResult refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", aliceRefresh))))
                .andExpect(status().isOk())
                .andReturn();
        String newAliceRefresh = objectMapper.readTree(refreshRes.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();
        assertThat(newAliceRefresh).isNotEqualTo(aliceRefresh);

        // Replaying old rotated token fails (401) AND revokes the new token in the same family
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", aliceRefresh))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", newAliceRefresh))))
                .andExpect(status().isUnauthorized());

        // 8. API Key creation & scoped enforcement
        MvcResult apiKeyRes = mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "BI Read Key",
                                "scopes", List.of("branch:read"),
                                "expiresInDays", 30
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String secretKey = objectMapper.readTree(apiKeyRes.getResponse().getContentAsString())
                .get("data").get("secretKey").asText();

        mockMvc.perform(get("/api/v1/branches")
                        .header("X-Api-Key", secretKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(post("/api/v1/branches")
                        .header("X-Api-Key", secretKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "DENY", "name", "Denied"))))
                .andExpect(status().isForbidden());

        // 9. TOTP MFA setup, enable, and challenge enforcement on login
        MvcResult mfaSetupRes = mockMvc.perform(post("/api/v1/auth/mfa/setup")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        String mfaSecret = objectMapper.readTree(mfaSetupRes.getResponse().getContentAsString())
                .get("data").get("secret").asText();

        String totpCode = totpService.generateCodeForTime(mfaSecret, Instant.now());
        mockMvc.perform(post("/api/v1/auth/mfa/enable")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", totpCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfaEnabled", is(true)));

        MvcResult mfaLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "alice@orga.test",
                                "password", "StrongPass@123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfaRequired", is(true)))
                .andReturn();
        String mfaToken = objectMapper.readTree(mfaLoginRes.getResponse().getContentAsString())
                .get("data").get("mfaToken").asText();

        String verifyCode = totpService.generateCodeForTime(mfaSecret, Instant.now());
        mockMvc.perform(post("/api/v1/auth/mfa/verify-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mfaToken", mfaToken,
                                "code", verifyCode
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 10. Audit logs recorded for Org A
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalElements").isNumber());
    }

    private JsonNode registerUser(String fullName, String email, String password, String orgName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "password", password,
                                "organizationName", orgName
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("session").get("accessToken").asText();
    }
}

