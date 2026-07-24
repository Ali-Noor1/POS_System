package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreSettingsResponse {

    private String storeName;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
}
