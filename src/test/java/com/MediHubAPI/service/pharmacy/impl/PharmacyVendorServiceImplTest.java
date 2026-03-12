package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.PharmacyVendorRowDto;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.repository.pharmacy.PharmacyVendorRepository;
import com.MediHubAPI.repository.projection.PharmacyVendorRowProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyVendorServiceImplTest {

    @Mock
    private PharmacyVendorRepository pharmacyVendorRepository;

    @InjectMocks
    private PharmacyVendorServiceImpl service;

    @Test
    @DisplayName("Get vendors accepts active as a supported sort field")
    void getVendorsAcceptsActiveSort() {
        when(pharmacyVendorRepository.searchVendors(
                eq(null),
                eq(Boolean.TRUE),
                eq("active"),
                eq("asc"),
                eq(PageRequest.of(0, 10))
        )).thenReturn(new PageImpl<>(List.of(vendorRow())));

        Page<PharmacyVendorRowDto> page = service.getVendors(null, true, 0, 10, "active,asc");

        assertThat(page.getContent()).hasSize(1);
        PharmacyVendorRowDto row = page.getContent().get(0);
        assertThat(row.getVendorId()).isEqualTo(11L);
        assertThat(row.getVendorName()).isEqualTo("Acme Pharma Supplies");
        assertThat(row.getActive()).isTrue();

        verify(pharmacyVendorRepository).searchVendors(
                null,
                true,
                "active",
                "asc",
                PageRequest.of(0, 10)
        );
    }

    @Test
    @DisplayName("Get vendors rejects unsupported sort fields")
    void getVendorsRejectsUnsupportedSortField() {
        assertThatThrownBy(() -> service.getVendors(null, null, 0, 10, "status,asc"))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException validationException = (ValidationException) ex;
                    assertThat(validationException.getDetails()).hasSize(1);
                    assertThat(validationException.getDetails().get(0).getField()).isEqualTo("sort");
                    assertThat(validationException.getDetails().get(0).getMessage()).isEqualTo("unsupported sort field");
                });
    }

    private PharmacyVendorRowProjection vendorRow() {
        return new PharmacyVendorRowProjection() {
            @Override
            public Long getVendorId() {
                return 11L;
            }

            @Override
            public String getVendorCode() {
                return "V-011";
            }

            @Override
            public String getVendorName() {
                return "Acme Pharma Supplies";
            }

            @Override
            public String getContactPerson() {
                return "Nandita";
            }

            @Override
            public String getPhone() {
                return "9876543210";
            }

            @Override
            public String getEmail() {
                return "vendor@example.com";
            }

            @Override
            public String getGstNo() {
                return "GST-123";
            }

            @Override
            public String getCity() {
                return "Chennai";
            }

            @Override
            public Integer getPaymentTermsDays() {
                return 30;
            }

            @Override
            public Boolean getActive() {
                return true;
            }
        };
    }
}
