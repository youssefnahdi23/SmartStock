package com.smartstock.supplier.unit;

import com.smartstock.supplier.api.dto.request.CreateContractRequest;
import com.smartstock.supplier.api.dto.response.ContractResponse;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.model.SupplierContract;
import com.smartstock.supplier.domain.repository.SupplierContractRepository;
import com.smartstock.supplier.exception.BusinessException;
import com.smartstock.supplier.service.SupplierContractService;
import com.smartstock.supplier.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierContractService Unit Tests")
class SupplierContractServiceUnitTest {

    @Mock
    private SupplierContractRepository contractRepository;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierContractService contractService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder()
                .id("sup-001")
                .supplierCode("SUPP-001")
                .supplierName("Acme Corp")
                .supplierType("MANUFACTURER")
                .isActive(true)
                .isVerified(false)
                .riskRating("MEDIUM")
                .currencyCode("USD")
                .countryCode("US")
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .createdBy("user-01")
                .updatedBy("user-01")
                .build();
    }

    @Nested
    @DisplayName("createContract")
    class CreateContract {

        @Test
        @DisplayName("creates contract when contract number is unique")
        void createContract_success() {
            CreateContractRequest req = new CreateContractRequest();
            req.setContractNumber("CONTRACT-001");
            req.setContractTitle("Master Supply Agreement");
            req.setContractType("MASTER_SUPPLY");
            req.setStartDate(LocalDate.now());
            req.setEndDate(LocalDate.now().plusYears(1));

            SupplierContract saved = SupplierContract.builder()
                    .id("contract-001")
                    .supplier(supplier)
                    .contractNumber("CONTRACT-001")
                    .contractTitle("Master Supply Agreement")
                    .contractType("MASTER_SUPPLY")
                    .startDate(req.getStartDate())
                    .endDate(req.getEndDate())
                    .discountPercentage(BigDecimal.ZERO)
                    .contractStatus("ACTIVE")
                    .approvalStatus("PENDING")
                    .createdBy("user-01")
                    .updatedBy("user-01")
                    .build();

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(contractRepository.existsByContractNumber("CONTRACT-001")).thenReturn(false);
            when(contractRepository.save(any(SupplierContract.class))).thenReturn(saved);

            ContractResponse result = contractService.createContract("sup-001", req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getContractNumber()).isEqualTo("CONTRACT-001");
            assertThat(result.getContractTitle()).isEqualTo("Master Supply Agreement");
            assertThat(result.getContractStatus()).isEqualTo("ACTIVE");
            verify(contractRepository).save(any(SupplierContract.class));
        }

        @Test
        @DisplayName("throws BusinessException when contract number already exists")
        void createContract_duplicateContractNumber() {
            CreateContractRequest req = new CreateContractRequest();
            req.setContractNumber("CONTRACT-001");
            req.setContractTitle("Duplicate Agreement");
            req.setContractType("SPOT");
            req.setStartDate(LocalDate.now());
            req.setEndDate(LocalDate.now().plusMonths(6));

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(contractRepository.existsByContractNumber("CONTRACT-001")).thenReturn(true);

            assertThatThrownBy(() -> contractService.createContract("sup-001", req, "user-01"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CONTRACT-001");

            verify(contractRepository, never()).save(any());
        }
    }
}
