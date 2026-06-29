package com.smartstock.product.unit;

import com.smartstock.product.api.dto.request.CreateProductRequest;
import com.smartstock.product.api.dto.response.ProductResponse;
import com.smartstock.product.domain.model.Product;
import com.smartstock.product.domain.repository.*;
import com.smartstock.product.exception.SkuAlreadyExistsException;
import com.smartstock.product.service.BarcodeService;
import com.smartstock.product.service.ProductEventPublisher;
import com.smartstock.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService unit tests")
class ProductServiceUnitTest {

    @Mock private ProductRepository       productRepository;
    @Mock private CategoryRepository      categoryRepository;
    @Mock private ProductBarcodeRepository barcodeRepository;
    @Mock private ProductAttributeRepository attributeRepository;
    @Mock private ProductSkuRepository    skuRepository;
    @Mock private BarcodeService          barcodeService;
    @Mock private ProductEventPublisher   eventPublisher;

    @InjectMocks
    private ProductService productService;

    private static final String USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        lenient().when(barcodeService.generateEan13()).thenReturn("1234567890128");
        lenient().when(barcodeRepository.existsByBarcodeValue(anyString())).thenReturn(false);
        lenient().when(barcodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("createProduct — happy path returns response with correct fields")
    void createProduct_happyPath_returnsResponse() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Test Widget")
                .sku("TWG-001")
                .unitPrice(new BigDecimal("99.99"))
                .unitCost(new BigDecimal("45.00"))
                .unit("PIECE")
                .build();

        when(productRepository.existsBySkuAndNotDeleted("TWG-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId("prod-001");
            return p;
        });

        ProductResponse response = productService.createProduct(req, USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getSku()).isEqualTo("TWG-001");
        assertThat(response.getName()).isEqualTo("Test Widget");
        assertThat(response.getUnitPrice()).isEqualByComparingTo("99.99");
        verify(eventPublisher).publishProductCreated(any());
    }

    @Test
    @DisplayName("createProduct — duplicate SKU throws SkuAlreadyExistsException")
    void createProduct_duplicateSku_throwsException() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Widget")
                .sku("DUPE-001")
                .unitPrice(BigDecimal.TEN)
                .unitCost(BigDecimal.ONE)
                .unit("PIECE")
                .build();

        when(productRepository.existsBySkuAndNotDeleted("DUPE-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(req, USER_ID))
                .isInstanceOf(SkuAlreadyExistsException.class)
                .hasMessageContaining("DUPE-001");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProduct — not found throws ProductNotFoundException")
    void getProduct_notFound_throwsException() {
        when(productRepository.findByIdAndNotDeleted("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct("missing"))
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("deactivateProduct — sets active=false and publishes event")
    void deactivateProduct_setsInactiveAndPublishesEvent() {
        Product product = Product.builder()
                .id("prod-001").sku("TWG-001").name("Widget")
                .active(true).lifecycleStatus("ACTIVE")
                .standardRetailPrice(BigDecimal.TEN).standardCost(BigDecimal.ONE)
                .unitOfMeasure("PIECE").createdBy(USER_ID).updatedBy(USER_ID)
                .build();

        when(productRepository.findByIdAndNotDeleted("prod-001")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.deactivateProduct("prod-001", USER_ID);

        assertThat(response.isActive()).isFalse();
        verify(eventPublisher).publishProductDeactivated(any());
    }

    @Test
    @DisplayName("reactivateProduct — sets active=true and publishes event")
    void reactivateProduct_setsActiveAndPublishesEvent() {
        Product product = Product.builder()
                .id("prod-001").sku("TWG-001").name("Widget")
                .active(false).lifecycleStatus("DISCONTINUED")
                .standardRetailPrice(BigDecimal.TEN).standardCost(BigDecimal.ONE)
                .unitOfMeasure("PIECE").createdBy(USER_ID).updatedBy(USER_ID)
                .build();

        when(productRepository.findByIdAndNotDeleted("prod-001")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.reactivateProduct("prod-001", USER_ID);

        assertThat(response.isActive()).isTrue();
        verify(eventPublisher).publishProductReactivated(any());
    }
}
