package com.smartstock.product.unit;

import com.smartstock.product.service.BarcodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BarcodeService unit tests")
class BarcodeServiceUnitTest {

    private final BarcodeService barcodeService = new BarcodeService();

    @RepeatedTest(10)
    @DisplayName("generateEan13 — produces 13-digit valid EAN-13")
    void generateEan13_producesValidValue() {
        String ean = barcodeService.generateEan13();
        assertThat(ean).hasSize(13).matches("\\d{13}");
    }

    @Test
    @DisplayName("encodeBarcodeToBase64 — EAN13 produces non-empty base64 image")
    void encodeBarcodeToBase64_ean13_producesImage() {
        String value = barcodeService.generateEan13();
        String result = barcodeService.encodeBarcodeToBase64(value, "EAN13");
        assertThat(result)
                .isNotBlank()
                .startsWith("data:image/png;base64,");
    }

    @Test
    @DisplayName("encodeQrCodeToBase64 — produces non-empty base64 image")
    void encodeQrCodeToBase64_producesImage() {
        String result = barcodeService.encodeQrCodeToBase64("smartstock://product/prod-001", 300);
        assertThat(result)
                .isNotBlank()
                .startsWith("data:image/png;base64,");
    }
}
