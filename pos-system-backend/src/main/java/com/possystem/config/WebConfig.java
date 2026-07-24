package com.possystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String productsUploadDirectory;
    private final String storeUploadDirectory;

    public WebConfig(
            @Value("${app.upload.products-dir}")
            String productsUploadDirectory,
            @Value("${app.upload.store-dir}")
            String storeUploadDirectory
    ) {
        this.productsUploadDirectory = productsUploadDirectory;
        this.storeUploadDirectory = storeUploadDirectory;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadPath = Paths.get(productsUploadDirectory)
                .toAbsolutePath()
                .normalize();

        String uploadLocation = uploadPath.toUri().toString();

        if (!uploadLocation.endsWith("/")) {
            uploadLocation = uploadLocation + "/";
        }

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(uploadLocation);

        Path storeUploadPath = Paths.get(storeUploadDirectory)
                .toAbsolutePath()
                .normalize();

        String storeUploadLocation = storeUploadPath.toUri().toString();

        if (!storeUploadLocation.endsWith("/")) {
            storeUploadLocation = storeUploadLocation + "/";
        }

        registry.addResourceHandler("/uploads/store/**")
                .addResourceLocations(storeUploadLocation);
    }
}
