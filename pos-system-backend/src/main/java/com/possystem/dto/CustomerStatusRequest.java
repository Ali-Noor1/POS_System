package com.possystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerStatusRequest {

    @NotBlank(message = "Customer status is required")
    @Pattern(
            regexp = "(?i)^(ACTIVE|INACTIVE)$",
            message = "Customer status must be ACTIVE or INACTIVE"
    )
    private String status;
}