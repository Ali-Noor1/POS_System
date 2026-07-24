package com.possystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusRequest {

    @NotBlank(message = "User status is required")
    @Pattern(
            regexp = "(?i)^\\s*(ACTIVE|INACTIVE)?\\s*$",
            message = "User status must be ACTIVE or INACTIVE"
    )
    private String status;
}
