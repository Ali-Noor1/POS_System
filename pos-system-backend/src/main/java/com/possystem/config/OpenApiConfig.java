package com.possystem.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI posSystemOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(
                        new SecurityRequirement().addList("bearerAuth")
                )
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. Do not write 'Bearer' manually.")
                        )
                )
                .info(new Info()
                        .title("POS System API")
                        .version("1.0.0")
                        .description("""
                                API documentation for the Retail POS and Inventory Management System.

                                Roles:
                                - ADMIN: manage products, users, inventory, reports
                                - CASHIER: create and manage sales
                                """)
                        .contact(new Contact()
                                .name("POS System Development Team")
                                .email("admin@possystem.local"))
                        .license(new License()
                                .name("Internal University Project")
                        )
                );
    }
}
