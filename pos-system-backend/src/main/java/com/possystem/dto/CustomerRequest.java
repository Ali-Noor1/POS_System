package com.possystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message = "Customer full name is required")
    @Size(max = 150, message = "Customer full name must not exceed 150 characters")
    private String fullName;

    @NotBlank(message = "Customer phone is required")
    @Size(max = 30, message = "Customer phone must not exceed 30 characters")
    @Pattern(
            regexp = "^[0-9+()\\-\\s]+$",
            message = "Phone number contains invalid characters"
    )
    private String phone;

    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Customer email must not exceed 150 characters")
    private String email;

    @Size(max = 500, message = "Customer address must not exceed 500 characters")
    private String address;
}