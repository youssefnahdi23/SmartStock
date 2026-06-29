package com.smartstock.product.api.controller;

import com.smartstock.product.api.dto.request.*;
import com.smartstock.product.api.dto.response.*;
import com.smartstock.product.security.SecurityUserDetails;
import com.smartstock.product.service.ImportExportService;
import com.smartstock.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Products", description = "Product catalog management")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final ImportExportService importExportService;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new product")
    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    public ResponseEntity<Map<String, Object>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        ProductResponse product = productService.createProduct(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(wrap(product));
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(wrap(productService.getProduct(productId)));
    }

    @Operation(summary = "Get product by SKU")
    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<Map<String, Object>> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(wrap(productService.getProductBySku(sku)));
    }

    @Operation(summary = "Get product by barcode")
    @GetMapping("/by-barcode/{barcode}")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<Map<String, Object>> getProductByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(wrap(productService.getProductByBarcode(barcode)));
    }

    @Operation(summary = "List products with filters and pagination")
    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<PagedResponse<ProductResponse>> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        size = Math.min(size, 100);
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, parts[0]));

        return ResponseEntity.ok(productService.listProducts(search, active, categoryId, pageable));
    }

    @Operation(summary = "Update product")
    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        return ResponseEntity.ok(wrap(productService.updateProduct(productId, request, principal.getUserId())));
    }

    @Operation(summary = "Deactivate a product")
    @PostMapping("/{productId}/deactivate")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<Map<String, Object>> deactivateProduct(
            @PathVariable String productId,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        return ResponseEntity.ok(wrap(productService.deactivateProduct(productId, principal.getUserId())));
    }

    @Operation(summary = "Reactivate a product")
    @PostMapping("/{productId}/reactivate")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<Map<String, Object>> reactivateProduct(
            @PathVariable String productId,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        return ResponseEntity.ok(wrap(productService.reactivateProduct(productId, principal.getUserId())));
    }

    // ── Barcodes ──────────────────────────────────────────────────────────────

    @Operation(summary = "Generate barcode for a product")
    @PostMapping("/{productId}/barcode")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<Map<String, Object>> generateBarcode(
            @PathVariable String productId,
            @Valid @RequestBody(required = false) GenerateBarcodeRequest request,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        if (request == null) request = new GenerateBarcodeRequest();
        return ResponseEntity.ok(wrap(productService.generateBarcode(productId, request, principal.getUserId())));
    }

    @Operation(summary = "Generate QR code for a product")
    @PostMapping("/{productId}/qrcode")
    @PreAuthorize("hasAuthority('product:write')")
    public ResponseEntity<Map<String, Object>> generateQrCode(
            @PathVariable String productId,
            @Valid @RequestBody(required = false) GenerateQrCodeRequest request,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        if (request == null) request = new GenerateQrCodeRequest();
        return ResponseEntity.ok(wrap(productService.generateQrCode(productId, request, principal.getUserId())));
    }

    // ── Import / Export ───────────────────────────────────────────────────────

    @Operation(summary = "Bulk import products from CSV")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('product:import')")
    public ResponseEntity<Map<String, Object>> importProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        ImportResultResponse result = importExportService.importCsv(file, categoryId, dryRun, principal.getUserId());
        return ResponseEntity.ok(wrap(result));
    }

    @Operation(summary = "Export products to CSV")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('product:export')")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Boolean active) {
        byte[] csv = importExportService.exportCsv(categoryId, active);
        String filename = "products-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Map<String, Object> wrap(Object data) {
        return Map.of("data", data, "meta", Map.of("timestamp", Instant.now().toString()));
    }
}
