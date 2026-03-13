package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.PharmacyTransactionDetailDto;
import com.MediHubAPI.dto.pharmacy.PharmacyTransactionRowDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface PharmacyTransactionQueryService {

    Page<PharmacyTransactionRowDto> getTransactions(String q,
                                                    Long medicineId,
                                                    Long vendorId,
                                                    String transactionType,
                                                    String batchNo,
                                                    String referenceType,
                                                    Long referenceId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    Integer page,
                                                    Integer size);

    PharmacyTransactionDetailDto getTransaction(Long transactionId);
}
