package com.MediHubAPI.dto.pharmacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyEnumOptionsDto {
    private List<String> purchaseOrderStatuses;
    private List<String> transactionTypes;
    private List<String> adjustmentReasons;
}
