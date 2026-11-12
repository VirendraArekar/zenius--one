package com.zenius.one.platform.config;

import com.zenius.one.modules.identity.UserEntity;
import com.zenius.one.modules.identity.UserRepository;
import com.zenius.one.modules.organization.OrganizationEntity;
import com.zenius.one.modules.organization.OrganizationProvisioningService;
import com.zenius.one.modules.organization.OrganizationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds development organizations, demo administrator credentials, and 1,040 business records
 * inside identity-org-service when running in local or dev profiles.
 */
@Configuration
@Profile({"dev", "local"})
public class DevSeedInitializer {

    @Bean
    CommandLineRunner seedDevelopmentData(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            OrganizationProvisioningService provisioningService,
            DemoDatasetSeeder demoDatasetSeeder
    ) {
        return args -> {
            UserEntity admin;
            OrganizationEntity primaryOrg;
            if (userRepository.count() == 0) {
                admin = new UserEntity();
                admin.setEmail("admin@zeniusone.local");
                admin.setFullName("Aarav Mehta");
                admin.setJobTitle("Chief Enterprise Architect");
                admin.setPasswordHash(passwordEncoder.encode("Zenius@2026!"));
                admin.setStatus("ACTIVE");
                admin = userRepository.save(admin);

                primaryOrg = provisioningService.createOrganizationWithDefaults(
                        admin.getId(),
                        admin.getEmail(),
                        "Acme Global Corp",
                        "acme-global",
                        "Enterprise Software",
                        "USD",
                        "America/New_York"
                );

                provisioningService.createOrganizationWithDefaults(
                        admin.getId(),
                        admin.getEmail(),
                        "Zenius India Pvt Ltd",
                        "zenius-india",
                        "SaaS & AI",
                        "INR",
                        "Asia/Kolkata"
                );
            } else {
                admin = userRepository.findAll().stream().findFirst().orElse(null);
                primaryOrg = organizationRepository.findAll().stream().findFirst().orElse(null);
            }

            if (admin != null) {
                for (OrganizationEntity org : organizationRepository.findAll()) {
                    demoDatasetSeeder.seedOrganization(org.getId(), admin.getId());
                }
            }
        };
    }
}

