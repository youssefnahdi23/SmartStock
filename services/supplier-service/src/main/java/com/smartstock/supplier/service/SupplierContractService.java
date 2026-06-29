package com.smartstock.supplier.service;

import com.smartstock.supplier.api.dto.request.CreateContractRequest;
import com.smartstock.supplier.api.dto.response.ContractResponse;
import com.smartstock.supplier.api.dto.response.PagedResponse;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.model.SupplierContract;
import com.smartstock.supplier.domain.repository.SupplierContractRepository;
import com.smartstock.supplier.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierContractService {

    private final SupplierContractRepository contractRepository;
    private final SupplierService supplierService;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_supplier:write')")
    public ContractResponse createContract(String supplierId, CreateContractRequest req, String actorId) {
        Supplier supplier = supplierService.findById(supplierId);

        if (contractRepository.existsByContractNumber(req.getContractNumber())) {
            throw new BusinessException("CONTRACT_NUMBER_EXISTS",
                    "Contract number already in use: " + req.getContractNumber(), HttpStatus.BAD_REQUEST);
        }

        SupplierContract contract = SupplierContract.builder()
                .supplier(supplier)
                .contractNumber(req.getContractNumber())
                .contractTitle(req.getContractTitle())
                .contractType(req.getContractType())
                .description(req.getDescription())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .renewalDate(req.getRenewalDate())
                .contractValue(req.getContractValue())
                .paymentTerms(req.getPaymentTerms())
                .discountPercentage(req.getDiscountPercentage() != null ? req.getDiscountPercentage() : BigDecimal.ZERO)
                .minimumVolume(req.getMinimumVolume())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        contract = contractRepository.save(contract);
        log.info("Contract created: supplierId={} contractId={} by={}", supplierId, contract.getId(), actorId);
        return toResponse(contract, supplier.getSupplierName());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_supplier:read')")
    public PagedResponse<ContractResponse> listContracts(String supplierId, Pageable pageable) {
        supplierService.findById(supplierId);
        Page<SupplierContract> page = contractRepository.findBySupplierId(supplierId, pageable);
        List<ContractResponse> items = page.getContent().stream()
                .map(c -> toResponse(c, null)).toList();
        return PagedResponse.<ContractResponse>builder()
                .data(items)
                .meta(PagedResponse.Meta.builder()
                        .timestamp(Instant.now())
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    private ContractResponse toResponse(SupplierContract c, String supplierName) {
        return ContractResponse.builder()
                .id(c.getId())
                .supplierId(c.getSupplier() != null ? c.getSupplier().getId() : null)
                .supplierName(supplierName)
                .contractNumber(c.getContractNumber())
                .contractTitle(c.getContractTitle())
                .contractType(c.getContractType())
                .description(c.getDescription())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .renewalDate(c.getRenewalDate())
                .contractValue(c.getContractValue())
                .paymentTerms(c.getPaymentTerms())
                .discountPercentage(c.getDiscountPercentage())
                .minimumVolume(c.getMinimumVolume())
                .contractStatus(c.getContractStatus())
                .approvalStatus(c.getApprovalStatus())
                .approvedBy(c.getApprovedBy())
                .approvedAt(c.getApprovedAt())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .expired(c.isExpired())
                .build();
    }
}
