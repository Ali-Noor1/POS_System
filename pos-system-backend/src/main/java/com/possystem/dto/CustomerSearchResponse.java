package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerSearchResponse {

    private Long id;
    private String fullName;
    private String phone;
}

