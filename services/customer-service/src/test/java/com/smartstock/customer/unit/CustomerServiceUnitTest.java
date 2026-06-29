package com.smartstock.customer.unit;

import com.smartstock.customer.api.dto.request.CreateCustomerRequest;
import com.smartstock.customer.api.dto.request.SuspendCustomerRequest;
import com.smartstock.customer.api.dto.request.UpdateCustomerRequest;
import com.smartstock.customer.api.dto.response.CustomerResponse;
import com.smartstock.customer.api.dto.response.CustomerSummaryResponse;
import com.smartstock.customer.api.dto.response.PagedResponse;
import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.repository.CustomerRepository;
import com.smartstock.customer.exception.CustomerCodeExistsException;
import com.smartstock.customer.exception.CustomerNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import com.smartstock.customer.service.CustomerEventPublisher;
import com.smartstock.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    @InjectMocks
    private CustomerService customerService;

    private Customer activeCustomer;

    @BeforeEach
    void setUp() {
        activeCustomer = Customer.builder()
                .id("cust-001")
                .customerCode("CUST-001")
                .customerName("ABC Corporation")
                .customerType("CORPORATE")
                .segment("ENTERPRISE")
                .isActive(true)
                .isVerified(false)
                .riskRating("LOW")
                .preferredCurrency("USD")
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .currentCreditBalance(BigDecimal.ZERO)
                .createdBy("user-01")
                .updatedBy("user-01")
                .build();
    }

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomer {

        @Test
        @DisplayName("creates customer when code is unique")
        void createCustomer_success() {
            CreateCustomerRequest req = new CreateCustomerRequest();
            req.setCustomerCode("CUST-001");
            req.setCustomerName("ABC Corporation");
            req.setCustomerType("CORPORATE");

            when(customerRepository.save(any(Customer.class))).thenReturn(activeCustomer);
            doNothing().when(eventPublisher).publishCustomerCreated(any());

            CustomerResponse result = customerService.createCustomer(req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getCustomerCode()).isEqualTo("CUST-001");
            assertThat(result.getCustomerName()).isEqualTo("ABC Corporation");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(customerRepository).save(any(Customer.class));
            verify(eventPublisher).publishCustomerCreated(any());
        }

        @Test
        @DisplayName("throws CustomerCodeExistsException on duplicate code (DB unique constraint)")
        void createCustomer_duplicateCode() {
            CreateCustomerRequest req = new CreateCustomerRequest();
            req.setCustomerCode("CUST-001");
            req.setCustomerName("ABC Corporation");

            when(customerRepository.save(any(Customer.class)))
                    .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

            assertThatThrownBy(() -> customerService.createCustomer(req, "user-01"))
                    .isInstanceOf(CustomerCodeExistsException.class);
        }
    }

    @Nested
    @DisplayName("getCustomer")
    class GetCustomer {

        @Test
        @DisplayName("returns customer when found")
        void getCustomer_found() {
            when(customerRepository.findById("cust-001")).thenReturn(Optional.of(activeCustomer));

            CustomerResponse result = customerService.getCustomer("cust-001");

            assertThat(result.getId()).isEqualTo("cust-001");
            assertThat(result.getCustomerCode()).isEqualTo("CUST-001");
        }

        @Test
        @DisplayName("throws CustomerNotFoundException when not found")
        void getCustomer_notFound() {
            when(customerRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomer("missing"))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomer {

        @Test
        @DisplayName("updates fields and publishes event")
        void updateCustomer_success() {
            UpdateCustomerRequest req = new UpdateCustomerRequest();
            req.setCustomerName("ABC Corporation Updated");
            req.setEmailAddress("updated@abccorp.com");

            when(customerRepository.findById("cust-001")).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any())).thenReturn(activeCustomer);
            doNothing().when(eventPublisher).publishCustomerUpdated(any());

            customerService.updateCustomer("cust-001", req, "user-01");

            verify(customerRepository).save(any());
            verify(eventPublisher).publishCustomerUpdated(any());
        }

        @Test
        @DisplayName("publishes segment changed event when segment changes")
        void updateCustomer_segmentChange() {
            UpdateCustomerRequest req = new UpdateCustomerRequest();
            req.setSegment("PREMIUM");

            when(customerRepository.findById("cust-001")).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishCustomerUpdated(any());
            doNothing().when(eventPublisher).publishCustomerSegmentChanged(any());

            customerService.updateCustomer("cust-001", req, "user-01");

            verify(eventPublisher).publishCustomerSegmentChanged(any());
        }
    }

    @Nested
    @DisplayName("suspendCustomer / resumeCustomer")
    class SuspendResume {

        @Test
        @DisplayName("suspends an active customer")
        void suspendCustomer_success() {
            SuspendCustomerRequest req = new SuspendCustomerRequest();
            req.setReason("Overdue payments");
            req.setResumeDate(LocalDate.now().plusDays(30));

            when(customerRepository.findById("cust-001")).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishCustomerSuspended(any());

            CustomerResponse result = customerService.suspendCustomer("cust-001", req, "user-01");

            assertThat(result.getStatus()).isEqualTo("SUSPENDED");
            verify(eventPublisher).publishCustomerSuspended(any());
        }

        @Test
        @DisplayName("resumes a suspended customer")
        void resumeCustomer_success() {
            activeCustomer.suspend("Overdue payments", LocalDate.now().plusDays(30));

            when(customerRepository.findById("cust-001")).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishCustomerResumed(any());

            CustomerResponse result = customerService.resumeCustomer("cust-001", "user-01");

            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(eventPublisher).publishCustomerResumed(any());
        }
    }

    @Nested
    @DisplayName("listCustomers")
    class ListCustomers {

        @Test
        @DisplayName("returns paged list of customers")
        void listCustomers_paged() {
            PageRequest pageable = PageRequest.of(0, 20);
            when(customerRepository.findWithFilters(any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(activeCustomer), pageable, 1));

            PagedResponse<CustomerSummaryResponse> result =
                    customerService.listCustomers(null, null, null, null, pageable);

            assertThat(result.getData()).hasSize(1);
            assertThat(result.getMeta().getTotal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Customer domain model")
    class CustomerDomainModel {

        @Test
        @DisplayName("resolves status correctly across lifecycle states")
        void statusResolution() {
            Customer c = Customer.builder()
                    .isActive(true)
                    .createdBy("u").updatedBy("u")
                    .customerCode("X").customerName("X")
                    .totalOrders(0).totalSpent(BigDecimal.ZERO)
                    .currentCreditBalance(BigDecimal.ZERO)
                    .build();

            assertThat(c.isActive()).isTrue();
            assertThat(c.isSuspended()).isFalse();
            assertThat(c.resolveStatus()).isEqualTo("ACTIVE");

            c.suspend("reason", LocalDate.now().plusDays(10));
            assertThat(c.isActive()).isFalse();
            assertThat(c.isSuspended()).isTrue();
            assertThat(c.resolveStatus()).isEqualTo("SUSPENDED");

            c.activate();
            assertThat(c.isActive()).isTrue();
            assertThat(c.isSuspended()).isFalse();
            assertThat(c.resolveStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("records order and updates counters correctly")
        void recordOrder() {
            Customer c = Customer.builder()
                    .isActive(true)
                    .createdBy("u").updatedBy("u")
                    .customerCode("X").customerName("X")
                    .totalOrders(5).totalSpent(BigDecimal.valueOf(5000))
                    .currentCreditBalance(BigDecimal.ZERO)
                    .build();

            c.recordOrder(BigDecimal.valueOf(1000));

            assertThat(c.getTotalOrders()).isEqualTo(6);
            assertThat(c.getTotalSpent()).isEqualByComparingTo(BigDecimal.valueOf(6000));
            assertThat(c.getLastOrderDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("calculates credit available correctly")
        void creditAvailable() {
            Customer c = Customer.builder()
                    .isActive(true)
                    .createdBy("u").updatedBy("u")
                    .customerCode("X").customerName("X")
                    .totalOrders(0).totalSpent(BigDecimal.ZERO)
                    .creditLimit(BigDecimal.valueOf(10000))
                    .currentCreditBalance(BigDecimal.valueOf(3000))
                    .build();

            assertThat(c.getCreditAvailable()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        }
    }
}
