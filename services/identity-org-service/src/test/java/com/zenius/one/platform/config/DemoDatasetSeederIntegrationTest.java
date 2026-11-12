package com.zenius.one.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDatasetSeederIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsOverThousandDummyRecordsIdempotentlyPerTenant() throws Exception {
        JsonNode tenantA = register("Seed Admin A", "seed-a@zenius.test", "StrongPass@123", "Seed Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("Seed Admin B", "seed-b@zenius.test", "StrongPass@123", "Seed Beta");
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        mockMvc.perform(post("/api/v1/security/seed-demo")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadySeeded", is(false)))
                .andExpect(jsonPath("$.data.totalRecordsCreated", greaterThanOrEqualTo(1200)));

        mockMvc.perform(post("/api/v1/security/seed-demo")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadySeeded", is(true)))
                .andExpect(jsonPath("$.data.totalRecordsCreated", is(0)));

        UUID uuidA = UUID.fromString(orgAId);
        UUID uuidB = UUID.fromString(orgBId);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_leads WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_deals WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(25);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hr_employees WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(60);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM proj_tasks WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(60);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM col_mail_messages WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM doc_folders WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bi_dashboards WHERE organization_id = ?", Integer.class, uuidA)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notifications WHERE organization_id = ?", Integer.class, uuidA)).isGreaterThanOrEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM proj_tasks WHERE organization_id = ?", Integer.class, uuidB)).isZero();
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

