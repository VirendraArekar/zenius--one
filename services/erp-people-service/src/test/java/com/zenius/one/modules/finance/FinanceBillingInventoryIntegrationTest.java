package com.zenius.one.modules.finance;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class FinanceBillingInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.zenius.one.TestTenantSupport testTenantSupport;

    @Test
    void doubleEntryFinanceSubscriptionsAndInventoryFlowsAndIsolation() throws Exception {
        JsonNode tenantA = register("CFO A", "cfo-a@zenius.test", "StrongPass@123", "Fin Alpha");
        String tokenA = tenantA.get("accessToken").asText();
        String orgAId = tenantA.get("user").get("activeOrganizationId").asText();

        JsonNode tenantB = register("CFO B", "cfo-b@zenius.test", "StrongPass@123", "Fin Beta");
        String tokenB = tenantB.get("accessToken").asText();
        String orgBId = tenantB.get("user").get("activeOrganizationId").asText();

        // 1. Finance: COA + Reject Unbalanced Journal
        MvcResult coaRes = mockMvc.perform(get("/api/v1/finance/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(10))))
                .andReturn();
        JsonNode coa = objectMapper.readTree(coaRes.getResponse().getContentAsString()).get("data");
        String cashAccId = coa.get(0).get("id").asText();
        String revAccId = coa.get(7).get("id").asText();

        mockMvc.perform(post("/api/v1/finance/journals")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Unbalanced Test",
                                "lines", List.of(
                                        Map.of("accountId", cashAccId, "debitAmount", 100, "creditAmount", 0),
                                        Map.of("accountId", revAccId, "debitAmount", 0, "creditAmount", 50)
                                )
                        ))))
                .andExpect(status().isBadRequest());

        // 2. Invoice with 18% GST + Payment + Bill with 10% TDS + Payment -> Reports
        MvcResult invRes = mockMvc.perform(post("/api/v1/finance/invoices")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerName", "GlobalTech",
                                "subtotal", new BigDecimal("1000.00"),
                                "gstRate", new BigDecimal("18.00")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gstAmount", is(180.0)))
                .andExpect(jsonPath("$.data.totalAmount", is(1180.0)))
                .andReturn();
        String invoiceId = objectMapper.readTree(invRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/finance/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("1180.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PAID")))
                .andExpect(jsonPath("$.data.balanceDue", is(0.0)));

        MvcResult billRes = mockMvc.perform(post("/api/v1/finance/bills")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "vendorName", "CloudHost Vendor",
                                "subtotal", new BigDecimal("500.00"),
                                "tdsRate", new BigDecimal("10.00")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tdsAmount", is(50.0)))
                .andExpect(jsonPath("$.data.totalAmount", is(450.0)))
                .andReturn();
        String billId = objectMapper.readTree(billRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/finance/bills/" + billId + "/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("450.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PAID")));

        mockMvc.perform(get("/api/v1/finance/reports")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue", is(1000.0)))
                .andExpect(jsonPath("$.data.totalExpense", is(500.0)))
                .andExpect(jsonPath("$.data.netProfit", is(500.0)))
                .andExpect(jsonPath("$.data.totalGstCollected", is(180.0)))
                .andExpect(jsonPath("$.data.totalTdsDeducted", is(50.0)));

        // 3. Billing & Subscriptions: Trial + Upgrade with Proration Credit
        MvcResult starterRes = mockMvc.perform(post("/api/v1/billing/plans")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "STARTER",
                                "name", "Starter Monthly",
                                "price", 49,
                                "trialDays", 14
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String starterId = objectMapper.readTree(starterRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult entRes = mockMvc.perform(post("/api/v1/billing/plans")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "ENTERPRISE",
                                "name", "Enterprise Monthly",
                                "price", 199,
                                "trialDays", 0
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String entId = objectMapper.readTree(entRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult subRes = mockMvc.perform(post("/api/v1/billing/subscriptions")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "planId", starterId,
                                "customerName", "Acme Subscriber",
                                "customerEmail", "billing@acme.io"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("TRIALING")))
                .andReturn();
        String subId = objectMapper.readTree(subRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/billing/subscriptions/" + subId + "/change-plan")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planId", entId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.planName", is("Enterprise Monthly")));

        // 4. Inventory & Commerce: Warehouses + Transfer + PO Receive + Store Checkout
        MvcResult whListRes = mockMvc.perform(get("/api/v1/inventory/warehouses")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andReturn();
        String mainWhId = objectMapper.readTree(whListRes.getResponse().getContentAsString()).get("data").get(0).get("id").asText();

        MvcResult blrWhRes = mockMvc.perform(post("/api/v1/inventory/warehouses")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "BLR",
                                "name", "Bengaluru Warehouse",
                                "city", "Bengaluru"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String blrWhId = objectMapper.readTree(blrWhRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult itemRes = mockMvc.perform(post("/api/v1/inventory/items")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", "LAPTOP-X1",
                                "name", "Enterprise Workstation X1",
                                "unitCost", 800,
                                "sellingPrice", 1200,
                                "initialQuantity", 10
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalQuantityOnHand", is(10)))
                .andReturn();
        String itemId = objectMapper.readTree(itemRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/inventory/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "itemId", itemId,
                                "fromWarehouseId", mainWhId,
                                "toWarehouseId", blrWhId,
                                "quantity", 4
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromWarehouseQuantity", is(6)))
                .andExpect(jsonPath("$.data.toWarehouseQuantity", is(4)));

        MvcResult poRes = mockMvc.perform(post("/api/v1/inventory/purchase-orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "vendorName", "Hardware OEM",
                                "warehouseId", blrWhId,
                                "itemId", itemId,
                                "quantity", 5,
                                "unitCost", 800
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String poId = objectMapper.readTree(poRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/inventory/purchase-orders/" + poId + "/receive")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RECEIVED")));

        mockMvc.perform(post("/api/v1/inventory/store-orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerName", "Online Buyer",
                                "customerEmail", "buyer@client.io",
                                "warehouseId", blrWhId,
                                "itemId", itemId,
                                "quantity", 2
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber", is("ORD-00001")))
                .andExpect(jsonPath("$.data.totalAmount", is(2400.0)));

        // Insufficient stock rejected
        mockMvc.perform(post("/api/v1/inventory/store-orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Organization-Id", orgAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerName", "Oversell Buyer",
                                "customerEmail", "buyer2@client.io",
                                "warehouseId", blrWhId,
                                "itemId", itemId,
                                "quantity", 50
                        ))))
                .andExpect(status().isConflict());

        // Tenant Isolation: Org B sees zero items & cannot cross into Org A finance
        mockMvc.perform(get("/api/v1/inventory/items")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/finance/reports")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Organization-Id", orgAId))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String fullName, String email, String password, String orgName) {
        return testTenantSupport.provisionTenant(fullName, email, orgName);
    }
}

