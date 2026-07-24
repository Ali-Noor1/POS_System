package com.possystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    void posSystemOpenAPI_configuresBearerSecurityRequirement() {

        OpenAPI openAPI = new OpenApiConfig().posSystemOpenAPI();

        assertNotNull(
                openAPI.getComponents()
                        .getSecuritySchemes()
                        .get("bearerAuth")
        );
        assertFalse(openAPI.getSecurity().isEmpty());
        assertEquals(
                "bearerAuth",
                openAPI.getSecurity().get(0).keySet().iterator().next()
        );
    }
}
