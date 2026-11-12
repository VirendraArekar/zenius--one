package com.zenius.one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.zenius.one")
public class WorkplaceAiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkplaceAiServiceApplication.class, args);
    }
}

