package com.zenius.one.platform.system;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemPlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void systemStatusReturnsSeededCountsAndOperationalState() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.platformName", is("Zenius One")))
                .andExpect(jsonPath("$.data.currentPhase", is(10)))
                .andExpect(jsonPath("$.data.status", is("OPERATIONAL")))
                .andExpect(jsonPath("$.data.totalModules", is(20)))
                .andExpect(jsonPath("$.data.activeModules", is(20)))
                .andExpect(jsonPath("$.data.plannedModules", is(0)))
                .andExpect(jsonPath("$.data.publicSettings", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    void listModulesSupportsPaginationAndFiltering() throws Exception {
        mockMvc.perform(get("/api/v1/system/modules")
                        .param("category", "REVENUE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.meta.totalElements", is(3)));
    }

    @Test
    void getModuleByCodeReturnsDetailsAndNotFoundProblem() throws Exception {
        mockMvc.perform(get("/api/v1/system/modules/ai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code", is("ai")))
                .andExpect(jsonPath("$.data.phaseNumber", is(9)));

        mockMvc.perform(get("/api/v1/system/modules/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    void unauthenticatedNonSystemApiIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/crm/leads"))
                .andExpect(status().isForbidden());
    }
}

