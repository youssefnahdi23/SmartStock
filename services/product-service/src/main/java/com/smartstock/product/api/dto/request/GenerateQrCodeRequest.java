package com.smartstock.product.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerateQrCodeRequest {

    @Min(value = 100, message = "QR code size must be at least 100px")
    @Max(value = 1000, message = "QR code size must not exceed 1000px")
    @Builder.Default
    private int size = 300;

    @Builder.Default
    private boolean regenerate = false;
}
