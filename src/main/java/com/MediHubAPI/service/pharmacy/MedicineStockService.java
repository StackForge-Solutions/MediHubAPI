package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.MedicineSearchResponse;
import com.MediHubAPI.dto.pharmacy.MedicineStockDto;
import com.MediHubAPI.enums.pharmacy.MedicineForm;
import com.MediHubAPI.enums.pharmacy.MedicineSearchMode;

public interface MedicineStockService {


    MedicineStockDto getStock(Long medicineId);

     MedicineSearchResponse search(MedicineSearchMode mode,
                                         MedicineForm form,
                                         String q,
                                         Integer limit,
                                         Boolean inStockOnly);
}