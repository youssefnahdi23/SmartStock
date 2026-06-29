package com.smartstock.product.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BarcodeResponse {

    private String productId;
    private String barcode;
    private String barcodeFormat;
    private String barcodeImage;
    private LocalDateTime generatedAt;
}
