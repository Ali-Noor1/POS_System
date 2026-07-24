package com.possystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "(?i)^(ACTIVE|INACTIVE)$",
            message = "Status must be ACTIVE or INACTIVE"
    )
    private String status;
}