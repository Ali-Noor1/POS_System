package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String roleName;
    private String status;
}