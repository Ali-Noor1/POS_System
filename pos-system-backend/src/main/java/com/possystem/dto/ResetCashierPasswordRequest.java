package com.possystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetCashierPasswordRequest {

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = "^\\s*$|^.{8,100}$",
            message = "New password must be at least 8 characters"
    )
    private String newPassword;
}
