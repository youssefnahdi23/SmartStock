package com.smartstock.product.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QrCodeResponse {

    private String productId;
    private String qrCode;
    private String qrCodeUrl;
    private String qrCodeImage;
    private int size;
    private LocalDateTime generatedAt;
}
