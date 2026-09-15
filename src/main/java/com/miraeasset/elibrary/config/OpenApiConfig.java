package com.miraeasset.elibrary.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eLibraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Library Service API")
                        .version("v1")
                        .description("Browse books and manage digital borrowing and returns."));
    }
}
