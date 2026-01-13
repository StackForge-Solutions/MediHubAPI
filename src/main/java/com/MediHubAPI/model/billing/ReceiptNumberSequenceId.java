 package com.MediHubAPI.model.billing;

import lombok.*;

import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ReceiptNumberSequenceId implements Serializable {
    private String clinicId;
    private int fy;
}
