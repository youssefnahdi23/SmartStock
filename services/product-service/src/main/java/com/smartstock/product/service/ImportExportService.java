package com.smartstock.product.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.smartstock.product.api.dto.request.CreateProductRequest;
import com.smartstock.product.api.dto.response.ImportResultResponse;
import com.smartstock.product.api.dto.response.PagedResponse;
import com.smartstock.product.api.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {

    private static final String[] CSV_HEADER = {
            "productId", "name", "sku", "categoryId", "categoryName",
            "unitPrice", "unitCost", "unit", "description", "active"
    };

    private final ProductService productService;

    @Transactional
    public ImportResultResponse importCsv(MultipartFile file, String categoryId,
                                          boolean dryRun, String userId) {
        String importId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();
        List<ImportResultResponse.ImportError> errors = new ArrayList<>();
        int rowsProcessed = 0;
        int rowsSuccessful = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] header = reader.readNext(); // skip header
            if (header == null) {
                return ImportResultResponse.builder()
                        .importId(importId).fileName(file.getOriginalFilename())
                        .rowsProcessed(0).rowsSuccessful(0).rowsFailed(0)
                        .errors(errors).dryRun(dryRun)
                        .startedAt(startedAt).completedAt(LocalDateTime.now())
                        .build();
            }

            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                rowsProcessed++;
                rowNum++;
                try {
                    if (row.length < 4) {
                        errors.add(new ImportResultResponse.ImportError(rowNum, "Insufficient columns"));
                        continue;
                    }
                    String name = row[0].trim();
                    String sku  = row.length > 1 ? row[1].trim() : null;
                    BigDecimal unitPrice = parseBigDecimal(row, 2, rowNum, errors);
                    BigDecimal unitCost  = parseBigDecimal(row, 3, rowNum, errors);

                    if (name.isBlank()) {
                        errors.add(new ImportResultResponse.ImportError(rowNum, "Name is required"));
                        continue;
                    }

                    if (!dryRun) {
                        CreateProductRequest req = CreateProductRequest.builder()
                                .name(name)
                                .sku(sku)
                                .categoryId(categoryId)
                                .unitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO)
                                .unitCost(unitCost != null ? unitCost : BigDecimal.ZERO)
                                .unit("PIECE")
                                .build();
                        productService.createProduct(req, userId);
                    }
                    rowsSuccessful++;
                } catch (Exception ex) {
                    log.warn("Import row {} failed: {}", rowNum, ex.getMessage());
                    errors.add(new ImportResultResponse.ImportError(rowNum, ex.getMessage()));
                }
            }
        } catch (IOException | CsvValidationException ex) {
            log.error("CSV import failed: {}", ex.getMessage());
            errors.add(new ImportResultResponse.ImportError(0, "File read error: " + ex.getMessage()));
        }

        return ImportResultResponse.builder()
                .importId(importId)
                .fileName(file.getOriginalFilename())
                .rowsProcessed(rowsProcessed)
                .rowsSuccessful(rowsSuccessful)
                .rowsFailed(rowsProcessed - rowsSuccessful)
                .errors(errors)
                .dryRun(dryRun)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public byte[] exportCsv(String categoryId, Boolean active) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos))) {

            writer.writeNext(CSV_HEADER);

            PagedResponse<ProductResponse> page = productService.listProducts(
                    null, active, categoryId, Pageable.ofSize(10_000));
            for (ProductResponse p : page.getData()) {
                writer.writeNext(new String[]{
                        p.getId(), p.getName(), p.getSku(),
                        p.getCategoryId() != null ? p.getCategoryId() : "",
                        p.getCategoryName() != null ? p.getCategoryName() : "",
                        p.getUnitPrice() != null ? p.getUnitPrice().toPlainString() : "0",
                        p.getUnitCost() != null ? p.getUnitCost().toPlainString() : "0",
                        p.getUnit(),
                        p.getDescription() != null ? p.getDescription() : "",
                        String.valueOf(p.isActive())
                });
            }
            writer.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate CSV export", ex);
        }
    }

    private BigDecimal parseBigDecimal(String[] row, int index, int rowNum,
                                        List<ImportResultResponse.ImportError> errors) {
        if (index >= row.length || row[index].isBlank()) return null;
        try {
            return new BigDecimal(row[index].trim());
        } catch (NumberFormatException ex) {
            errors.add(new ImportResultResponse.ImportError(rowNum,
                    "Invalid number at column " + (index + 1) + ": " + row[index]));
            return null;
        }
    }
}
