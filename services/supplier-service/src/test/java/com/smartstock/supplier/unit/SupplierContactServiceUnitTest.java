package com.smartstock.supplier.unit;

import com.smartstock.supplier.api.dto.request.CreateContactRequest;
import com.smartstock.supplier.api.dto.response.ContactResponse;
import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.model.SupplierContact;
import com.smartstock.supplier.domain.repository.SupplierContactRepository;
import com.smartstock.supplier.exception.BusinessException;
import com.smartstock.supplier.service.SupplierContactService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierContactService Unit Tests")
class SupplierContactServiceUnitTest {

    @Mock
    private SupplierContactRepository contactRepository;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierContactService contactService;

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
    @DisplayName("addContact")
    class AddContact {

        @Test
        @DisplayName("adds a primary contact and demotes any existing primary")
        void addContact_primaryPromoted_existingDemoted() {
            SupplierContact existingPrimary = SupplierContact.builder()
                    .id("contact-old")
                    .supplier(supplier)
                    .contactName("Old Primary")
                    .isPrimary(true)
                    .isActive(true)
                    .build();

            SupplierContact saved = SupplierContact.builder()
                    .id("contact-new")
                    .supplier(supplier)
                    .contactName("New Primary")
                    .contactType("GENERAL")
                    .isPrimary(true)
                    .isActive(true)
                    .build();

            CreateContactRequest req = new CreateContactRequest();
            req.setContactName("New Primary");
            req.setIsPrimary(true);

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(contactRepository.findBySupplierIdAndIsPrimaryTrue("sup-001"))
                    .thenReturn(Optional.of(existingPrimary));
            when(contactRepository.save(existingPrimary)).thenReturn(existingPrimary);
            when(contactRepository.save(argThat(c -> "New Primary".equals(c.getContactName()))))
                    .thenReturn(saved);

            ContactResponse result = contactService.addContact("sup-001", req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getContactName()).isEqualTo("New Primary");
            assertThat(result.getIsPrimary()).isTrue();

            // existing primary must have been demoted
            assertThat(existingPrimary.getIsPrimary()).isFalse();
            verify(contactRepository, times(2)).save(any(SupplierContact.class));
        }

        @Test
        @DisplayName("adds a non-primary contact without touching existing primary")
        void addContact_nonPrimary_noExistingDemoted() {
            SupplierContact saved = SupplierContact.builder()
                    .id("contact-new")
                    .supplier(supplier)
                    .contactName("Support Contact")
                    .contactType("GENERAL")
                    .isPrimary(false)
                    .isActive(true)
                    .build();

            CreateContactRequest req = new CreateContactRequest();
            req.setContactName("Support Contact");
            req.setIsPrimary(false);

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(contactRepository.save(any(SupplierContact.class))).thenReturn(saved);

            ContactResponse result = contactService.addContact("sup-001", req, "user-01");

            assertThat(result).isNotNull();
            assertThat(result.getIsPrimary()).isFalse();
            verify(contactRepository, never()).findBySupplierIdAndIsPrimaryTrue(any());
        }
    }

    @Nested
    @DisplayName("deactivateContact")
    class DeactivateContact {

        @Test
        @DisplayName("deactivates contact when found for the supplier")
        void deactivateContact_success() {
            SupplierContact contact = SupplierContact.builder()
                    .id("contact-001")
                    .supplier(supplier)
                    .contactName("Jane Doe")
                    .isActive(true)
                    .build();

            when(contactRepository.findById("contact-001")).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(SupplierContact.class))).thenReturn(contact);

            contactService.deactivateContact("sup-001", "contact-001", "user-01");

            assertThat(contact.getIsActive()).isFalse();
            verify(contactRepository).save(contact);
        }

        @Test
        @DisplayName("throws BusinessException when contact not found")
        void deactivateContact_notFound() {
            when(contactRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.deactivateContact("sup-001", "missing", "user-01"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws BusinessException when contact belongs to a different supplier")
        void deactivateContact_wrongSupplier() {
            SupplierContact contact = SupplierContact.builder()
                    .id("contact-001")
                    .supplier(Supplier.builder().id("other-supplier").supplierCode("X").supplierName("X")
                            .totalOrders(0).totalSpent(BigDecimal.ZERO).createdBy("u").updatedBy("u").build())
                    .contactName("Jane Doe")
                    .isActive(true)
                    .build();

            when(contactRepository.findById("contact-001")).thenReturn(Optional.of(contact));

            assertThatThrownBy(() -> contactService.deactivateContact("sup-001", "contact-001", "user-01"))
                    .isInstanceOf(BusinessException.class);

            verify(contactRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listContacts")
    class ListContacts {

        @Test
        @DisplayName("returns active contacts for supplier")
        void listContacts_returnsActiveContacts() {
            SupplierContact c1 = SupplierContact.builder()
                    .id("c1").supplier(supplier).contactName("Alice").isActive(true).isPrimary(true).contactType("GENERAL").build();
            SupplierContact c2 = SupplierContact.builder()
                    .id("c2").supplier(supplier).contactName("Bob").isActive(true).isPrimary(false).contactType("GENERAL").build();

            when(supplierService.findById("sup-001")).thenReturn(supplier);
            when(contactRepository.findBySupplierIdAndIsActiveTrue("sup-001")).thenReturn(List.of(c1, c2));

            List<ContactResponse> result = contactService.listContacts("sup-001");

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ContactResponse::getContactName).containsExactly("Alice", "Bob");
        }
    }
}
