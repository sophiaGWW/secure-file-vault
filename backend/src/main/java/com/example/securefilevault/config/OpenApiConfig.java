package com.example.securefilevault.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI secureFileVaultOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secure File Vault API")
                        .version("v1")
                        .description("Phase 1 API for the S3-based permission-controlled file management system."));
    }
}
