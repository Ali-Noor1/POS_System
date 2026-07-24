package com.possystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreSettingsRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 120, message = "Store name must not exceed 120 characters")
    private String storeName;

    @Size(max = 500, message = "Store address must not exceed 500 characters")
    private String address;

    @Size(max = 40, message = "Store phone must not exceed 40 characters")
    private String phone;

    @Email(message = "Store email format is invalid")
    @Size(max = 150, message = "Store email must not exceed 150 characters")
    private String email;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;
}
