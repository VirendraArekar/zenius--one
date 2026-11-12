package com.zenius.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @RestController
    static class GatewayTopologyController {

        @GetMapping("/api/v1/system/microservices")
        public Mono<Map<String, Object>> topology() {
            return Mono.just(Map.of(
                    "success", true,
                    "data", List.of(
                            Map.of("service", "gateway-service", "port", 8080, "role", "Spring Cloud API Gateway"),
                            Map.of("service", "identity-org-service", "port", 8081, "role", "Identity, SSO, Multi-Tenancy, RBAC & IT Security"),
                            Map.of("service", "customer-service", "port", 8082, "role", "CRM, Sales, Marketing & Customer Support"),
                            Map.of("service", "erp-people-service", "port", 8083, "role", "Finance, Billing, Inventory, HR, Recruitment & Payroll"),
                            Map.of("service", "workplace-ai-service", "port", 8084, "role", "Projects, Collaboration, Docs, Legal, BI, Creator, Automation & Zenius AI")
                    )
            ));
        }
    }
}

