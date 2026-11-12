package com.zenius.one.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zeniusOneOpenApi(
            @Value("${zenius.platform.version:0.1.0}") String platformVersion
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("Zenius One Enterprise API")
                        .version(platformVersion)
                        .description("Unified Enterprise Business Operating System REST API"));
    }
}

