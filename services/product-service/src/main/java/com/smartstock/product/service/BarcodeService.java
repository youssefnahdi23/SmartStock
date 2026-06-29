package com.smartstock.product.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.*;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.smartstock.product.exception.BusinessException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class BarcodeService {

    private static final Random RANDOM = new Random();

    public String generateEan13() {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        digits.append(calculateEan13CheckDigit(digits.toString()));
        return digits.toString();
    }

    public String generateCode128() {
        return "SS" + System.currentTimeMillis() + RANDOM.nextInt(1000);
    }

    public String encodeBarcodeToBase64(String value, String format) {
        try {
            BarcodeFormat barcodeFormat = toBarcodeFormat(format);
            BitMatrix matrix = createMatrix(value, barcodeFormat, 300, 100);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            return "data:image/png;base64," + toBase64(image);
        } catch (Exception ex) {
            log.error("Failed to generate barcode image: {}", ex.getMessage());
            throw new BusinessException("BARCODE_GENERATION_FAILED",
                    "Failed to generate barcode image", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String encodeQrCodeToBase64(String data, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            return "data:image/png;base64," + toBase64(image);
        } catch (Exception ex) {
            log.error("Failed to generate QR code image: {}", ex.getMessage());
            throw new BusinessException("QRCODE_GENERATION_FAILED",
                    "Failed to generate QR code image", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private BitMatrix createMatrix(String value, BarcodeFormat format, int width, int height) throws WriterException {
        Writer writer = switch (format) {
            case EAN_13  -> new EAN13Writer();
            case EAN_8   -> new EAN8Writer();
            case UPC_A   -> new UPCAWriter();
            case CODE_128 -> new Code128Writer();
            case CODE_39  -> new Code39Writer();
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
        return writer.encode(value, format, width, height);
    }

    private BarcodeFormat toBarcodeFormat(String format) {
        return switch (format.toUpperCase()) {
            case "EAN13"   -> BarcodeFormat.EAN_13;
            case "EAN8"    -> BarcodeFormat.EAN_8;
            case "UPCA"    -> BarcodeFormat.UPC_A;
            case "CODE128" -> BarcodeFormat.CODE_128;
            case "CODE39"  -> BarcodeFormat.CODE_39;
            default -> BarcodeFormat.EAN_13;
        };
    }

    private String toBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private char calculateEan13CheckDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = digits.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return (char) ('0' + check);
    }
}
