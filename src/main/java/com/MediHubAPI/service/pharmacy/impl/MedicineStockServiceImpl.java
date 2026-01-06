package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.MedicineSearchItemDto;
import com.MediHubAPI.dto.pharmacy.MedicineSearchMeta;
import com.MediHubAPI.dto.pharmacy.MedicineSearchResponse;
import com.MediHubAPI.dto.pharmacy.MedicineStockDto;
import com.MediHubAPI.enums.pharmacy.MedicineForm;
import com.MediHubAPI.enums.pharmacy.MedicineSearchMode;
import com.MediHubAPI.repository.pharmacy.MdmMedicineRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockRepository;
import com.MediHubAPI.repository.projection.MedicineSearchRowProjection;
import com.MediHubAPI.service.pharmacy.MedicineStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import com.MediHubAPI.exception.pharmacy.MedicineNotFoundException;
import com.MediHubAPI.model.mdm.MdmMedicine;
import com.MediHubAPI.model.pharmacy.PharmacyStock;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineStockServiceImpl implements MedicineStockService {

    private final MdmMedicineRepository medicineSearchRepository;
    private final PharmacyStockRepository pharmacyStockRepository;

    @Override
    public MedicineSearchResponse search(MedicineSearchMode mode,
                                         MedicineForm form,
                                         String q,
                                         Integer limit,
                                         Boolean inStockOnly) {

        // -------- Validate ----------
        if (q == null || q.trim().length() < 2) {
            throw new IllegalArgumentException("q must be at least 2 characters");
        }

        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        boolean onlyStock = Boolean.TRUE.equals(inStockOnly);

        PageRequest page = PageRequest.of(0, safeLimit);

        String formStr = (form == null) ? null : form.name();

        List<MedicineSearchRowProjection> rows = (mode == MedicineSearchMode.BRAND)
                ? medicineSearchRepository.searchByBrand(q.trim(), formStr, onlyStock, page)
                : medicineSearchRepository.searchByComposition(q.trim(), formStr, onlyStock, page);

        List<MedicineSearchItemDto> data = rows.stream()
                .map(r -> MedicineSearchItemDto.builder()
                        .id(r.getId())
                        .form(r.getForm())
                        .brand(r.getBrand())
                        .composition(r.getComposition())
                        .stockQty(r.getStockQty() == null ? 0 : r.getStockQty())
                        .inStock(r.getInStock() != null && r.getInStock() == 1)
                        .build())
                .collect(Collectors.toList());

        MedicineSearchMeta meta = MedicineSearchMeta.builder()
                .mode(mode.name())
                .form(form == null ? null : form.name())
                .q(q.trim())
                .limit(safeLimit)
                .inStockOnly(onlyStock)
                .returned(data.size())
                .build();

        log.info("Medicine search: mode={}, form={}, q={}, limit={}, inStockOnly={}, returned={}",
                mode, form, q, safeLimit, onlyStock, data.size());

        return MedicineSearchResponse.builder()
                .data(data)
                .meta(meta)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicineStockDto getStock(Long medicineId) {

        // Ensure medicine exists (so we can throw MEDICINE_NOT_FOUND)
        MdmMedicine med = medicineSearchRepository.findById(medicineId)
                .orElseThrow(() -> new MedicineNotFoundException(medicineId));

        PharmacyStock stock = pharmacyStockRepository.findByMedicine_Id(medicineId).orElse(null);

        int qty = (stock == null || stock.getAvailableQty() == null) ? 0 : stock.getAvailableQty();
        boolean inStock = qty > 0;

        Instant updatedAt = null;
        // If you added updatedAt column:
        if (stock != null && stock.getUpdatedAt() != null) {
            updatedAt = stock.getUpdatedAt().toInstant(ZoneOffset.UTC);
        }

        log.info("Medicine stock: medicineId={}, brand={}, qty={}, inStock={}",
                medicineId, med.getBrand(), qty, inStock);

        return MedicineStockDto.builder()
                .medicineId(medicineId)
                .stockQty(qty)
                .inStock(inStock)
                .lastUpdatedAt(updatedAt)
                .build();
    }
}
