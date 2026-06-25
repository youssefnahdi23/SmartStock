package com.smartstock.product.api.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerateBarcodeRequest {

    @Pattern(regexp = "EAN13|EAN8|UPCA|CODE128|CODE39",
             message = "Barcode format must be one of: EAN13, EAN8, UPCA, CODE128, CODE39")
    @Builder.Default
    private String barcodeFormat = "EAN13";

    @Builder.Default
    private boolean regenerate = false;
}
