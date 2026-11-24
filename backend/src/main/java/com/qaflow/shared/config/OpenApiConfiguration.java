package com.qaflow.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI qaFlowOpenApi() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT HS256 de 15 minutos con validación de usuario activo y versión de autorización.");

        return new OpenAPI()
                .info(new Info()
                        .title("QAFlow Manufacturing API")
                        .version("v1")
                        .description("API REST del MVP de manufactura y portafolio QA Automation/DevOps.")
                        .contact(new Contact().name("QAFlow Portfolio")))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth));
    }
}
