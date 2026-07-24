package com.possystem.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppSettingsResponse {

    private StoreSettingsResponse store;
    private ReceiptSettingsResponse receipt;
}
