package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerResponse {

    private Long id;

    private String fullName;
    private String phone;
    private String email;
    private String address;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}