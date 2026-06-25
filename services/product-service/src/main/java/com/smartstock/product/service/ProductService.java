package com.smartstock.product.service;

import com.smartstock.product.api.dto.request.CreateProductRequest;
import com.smartstock.product.api.dto.request.GenerateBarcodeRequest;
import com.smartstock.product.api.dto.request.GenerateQrCodeRequest;
import com.smartstock.product.api.dto.request.UpdateProductRequest;
import com.smartstock.product.api.dto.response.*;
import com.smartstock.product.domain.event.BarcodeGeneratedEvent;
import com.smartstock.product.domain.event.ProductCreatedEvent;
import com.smartstock.product.domain.event.ProductDeactivatedEvent;
import com.smartstock.product.domain.event.ProductReactivatedEvent;
import com.smartstock.product.domain.event.ProductUpdatedEvent;
import com.smartstock.product.domain.model.*;
import com.smartstock.product.domain.repository.*;
import com.smartstock.product.exception.CategoryNotFoundException;
import com.smartstock.product.exception.ProductNotFoundException;
import com.smartstock.product.exception.SkuAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductBarcodeRepository barcodeRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductSkuRepository skuRepository;
    private final BarcodeService barcodeService;
    private final ProductEventPublisher eventPublisher;

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req, String userId) {
        String sku = resolveSku(req.getSku());
        if (productRepository.existsBySkuAndNotDeleted(sku)) {
            throw new SkuAlreadyExistsException(sku);
        }

        Category category = resolveCategory(req.getCategoryId());

        Product product = Product.builder()
                .sku(sku)
                .name(req.getName())
                .description(req.getDescription())
                .manufacturer(req.getManufacturer())
                .brand(req.getBrand())
                .unitOfMeasure(req.getUnit() != null ? req.getUnit() : "PIECE")
                .standardRetailPrice(req.getUnitPrice())
                .standardCost(req.getUnitCost())
                .weight(req.getWeight())
                .weightUnit(req.getWeightUnit())
                .reorderLevel(req.getReorderPoint() != null ? req.getReorderPoint() : 10)
                .reorderQuantity(req.getReorderQuantity() != null ? req.getReorderQuantity() : 50)
                .maxStock(req.getMaxStock() != null ? req.getMaxStock() : 0)
                .active(req.isActive())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        if (req.getDimensions() != null) {
            product.setLength(req.getDimensions().getLength());
            product.setWidth(req.getDimensions().getWidth());
            product.setHeight(req.getDimensions().getHeight());
            product.setDimensionUnit(req.getDimensions().getUnit());
        }

        product = productRepository.save(product);

        if (category != null) {
            ProductCategory pc = ProductCategory.builder()
                    .product(product)
                    .category(category)
                    .primary(true)
                    .assignedBy(userId)
                    .build();
            product.getProductCategories().add(pc);
        }

        if (req.getAttributes() != null) {
            saveAttributes(product, req.getAttributes(), userId);
        }

        ProductBarcode barcode = generateAndSaveBarcode(product, "EAN13", userId);

        product = productRepository.save(product);

        log.info("Product created: id={}, sku={}", product.getId(), product.getSku());

        ProductCreatedEvent event = new ProductCreatedEvent(
                product.getId(), product.getSku(), product.getName(),
                product.getDescription(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                product.getStandardRetailPrice(), product.getStandardCost(),
                product.getUnitOfMeasure(), product.getWeight(),
                barcode.getBarcodeValue(), userId);
        eventPublisher.publishProductCreated(event);

        return toResponse(product);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySkuAndNotDeleted(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcodeValue(barcode)
                .orElseThrow(() -> new ProductNotFoundException("barcode:" + barcode));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> listProducts(String search, Boolean active,
                                                        String categoryId, Pageable pageable) {
        Page<Product> page = productRepository.findAllWithFilters(search, active, categoryId, pageable);
        return PagedResponse.of(page, page.getContent().stream().map(this::toResponse).toList());
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse updateProduct(String productId, UpdateProductRequest req, String userId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Map<String, Object> previousValues = captureSnapshot(product);

        if (req.getName() != null)        product.setName(req.getName());
        if (req.getDescription() != null)  product.setDescription(req.getDescription());
        if (req.getManufacturer() != null) product.setManufacturer(req.getManufacturer());
        if (req.getBrand() != null)        product.setBrand(req.getBrand());
        if (req.getUnitPrice() != null)    product.setStandardRetailPrice(req.getUnitPrice());
        if (req.getUnitCost() != null)     product.setStandardCost(req.getUnitCost());
        if (req.getWeight() != null)       product.setWeight(req.getWeight());
        if (req.getWeightUnit() != null)   product.setWeightUnit(req.getWeightUnit());
        if (req.getReorderPoint() != null)    product.setReorderLevel(req.getReorderPoint());
        if (req.getReorderQuantity() != null) product.setReorderQuantity(req.getReorderQuantity());
        if (req.getMaxStock() != null)        product.setMaxStock(req.getMaxStock());

        if (req.getDimensions() != null) {
            product.setLength(req.getDimensions().getLength());
            product.setWidth(req.getDimensions().getWidth());
            product.setHeight(req.getDimensions().getHeight());
            product.setDimensionUnit(req.getDimensions().getUnit());
        }

        if (req.getCategoryId() != null) {
            Category category = resolveCategory(req.getCategoryId());
            product.getProductCategories().clear();
            if (category != null) {
                ProductCategory pc = ProductCategory.builder()
                        .product(product)
                        .category(category)
                        .primary(true)
                        .assignedBy(userId)
                        .build();
                product.getProductCategories().add(pc);
            }
        }

        if (req.getAttributes() != null) {
            attributeRepository.deleteAllByProductId(productId);
            saveAttributes(product, req.getAttributes(), userId);
        }

        product.setUpdatedBy(userId);
        product = productRepository.save(product);

        Map<String, Object> changes = captureSnapshot(product);
        eventPublisher.publishProductUpdated(new ProductUpdatedEvent(
                product.getId(), changes, previousValues, userId));

        log.info("Product updated: id={}", product.getId());
        return toResponse(product);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse deactivateProduct(String productId, String userId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.deactivate(userId);
        product = productRepository.save(product);

        eventPublisher.publishProductDeactivated(
                new ProductDeactivatedEvent(product.getId(), product.getSku(),
                        product.getName(), "Deactivated by user", userId));

        log.info("Product deactivated: id={}, sku={}", product.getId(), product.getSku());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse reactivateProduct(String productId, String userId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.reactivate(userId);
        product = productRepository.save(product);

        eventPublisher.publishProductReactivated(
                new ProductReactivatedEvent(product.getId(), product.getSku(),
                        product.getName(), userId));

        log.info("Product reactivated: id={}, sku={}", product.getId(), product.getSku());
        return toResponse(product);
    }

    // ── Barcode ───────────────────────────────────────────────────────────────

    @Transactional
    public BarcodeResponse generateBarcode(String productId, GenerateBarcodeRequest req, String userId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!req.isRegenerate()) {
            Optional<ProductBarcode> existing = barcodeRepository.findPrimaryByProductId(productId);
            if (existing.isPresent()) {
                ProductBarcode b = existing.get();
                return BarcodeResponse.builder()
                        .productId(productId)
                        .barcode(b.getBarcodeValue())
                        .barcodeFormat(b.getBarcodeFormat())
                        .barcodeImage(barcodeService.encodeBarcodeToBase64(b.getBarcodeValue(), b.getBarcodeFormat()))
                        .generatedAt(b.getCreatedAt())
                        .build();
            }
        }

        String format = req.getBarcodeFormat() != null ? req.getBarcodeFormat() : "EAN13";
        ProductBarcode barcode = generateAndSaveBarcode(product, format, userId);
        productRepository.save(product);

        eventPublisher.publishBarcodeGenerated(
                new BarcodeGeneratedEvent(productId, product.getSku(),
                        barcode.getBarcodeValue(), format, userId));

        return BarcodeResponse.builder()
                .productId(productId)
                .barcode(barcode.getBarcodeValue())
                .barcodeFormat(format)
                .barcodeImage(barcodeService.encodeBarcodeToBase64(barcode.getBarcodeValue(), format))
                .generatedAt(barcode.getCreatedAt())
                .build();
    }

    @Transactional
    public QrCodeResponse generateQrCode(String productId, GenerateQrCodeRequest req, String userId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        String qrData = "smartstock://product/" + productId;
        String qrImage = barcodeService.encodeQrCodeToBase64(qrData, req.getSize());

        return QrCodeResponse.builder()
                .productId(productId)
                .qrCode("QR-" + product.getSku())
                .qrCodeUrl(qrData)
                .qrCodeImage(qrImage)
                .size(req.getSize())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveSku(String requestedSku) {
        if (requestedSku != null && !requestedSku.isBlank()) {
            return requestedSku.trim().toUpperCase();
        }
        return "SS-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
    }

    private Category resolveCategory(String categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndActive(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private ProductBarcode generateAndSaveBarcode(Product product, String format, String userId) {
        String value = "EAN13".equals(format)
                ? barcodeService.generateEan13()
                : barcodeService.generateCode128();

        while (barcodeRepository.existsByBarcodeValue(value)) {
            value = "EAN13".equals(format)
                    ? barcodeService.generateEan13()
                    : barcodeService.generateCode128();
        }

        ProductBarcode barcode = ProductBarcode.builder()
                .product(product)
                .barcodeValue(value)
                .barcodeType(format)
                .barcodeFormat(format)
                .primary(true)
                .createdBy(userId)
                .build();
        barcodeRepository.save(barcode);
        product.getBarcodes().add(barcode);
        return barcode;
    }

    private void saveAttributes(Product product, Map<String, String> attributes, String userId) {
        int order = 0;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            ProductAttribute attr = ProductAttribute.builder()
                    .product(product)
                    .name(entry.getKey())
                    .value(entry.getValue())
                    .sortOrder(order++)
                    .updatedBy(userId)
                    .build();
            product.getAttributes().add(attr);
        }
    }

    private Map<String, Object> captureSnapshot(Product p) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("name", p.getName());
        snap.put("description", p.getDescription());
        snap.put("unitPrice", p.getStandardRetailPrice());
        snap.put("unitCost", p.getStandardCost());
        snap.put("lifecycleStatus", p.getLifecycleStatus());
        return snap;
    }

    ProductResponse toResponse(Product product) {
        Category primaryCategory = product.getPrimaryCategory();
        ProductBarcode primaryBarcode = product.getPrimaryBarcode();

        Map<String, String> attributes = product.getAttributes().stream()
                .collect(Collectors.toMap(
                        ProductAttribute::getName,
                        ProductAttribute::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));

        ProductResponse.DimensionsResponse dims = null;
        if (product.getLength() != null || product.getWidth() != null || product.getHeight() != null) {
            dims = ProductResponse.DimensionsResponse.builder()
                    .length(product.getLength())
                    .width(product.getWidth())
                    .height(product.getHeight())
                    .unit(product.getDimensionUnit())
                    .build();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .manufacturer(product.getManufacturer())
                .brand(product.getBrand())
                .categoryId(primaryCategory != null ? primaryCategory.getId() : null)
                .categoryName(primaryCategory != null ? primaryCategory.getName() : null)
                .unitPrice(product.getStandardRetailPrice())
                .unitCost(product.getStandardCost())
                .unit(product.getUnitOfMeasure())
                .weight(product.getWeight())
                .weightUnit(product.getWeightUnit())
                .dimensions(dims)
                .reorderPoint(product.getReorderLevel())
                .reorderQuantity(product.getReorderQuantity())
                .maxStock(product.getMaxStock())
                .barcode(primaryBarcode != null ? primaryBarcode.getBarcodeValue() : null)
                .barcodeFormat(primaryBarcode != null ? primaryBarcode.getBarcodeFormat() : null)
                .attributes(attributes)
                .active(product.isActive())
                .lifecycleStatus(product.getLifecycleStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
