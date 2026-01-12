package com.MediHubAPI.model.billing;

import java.io.Serializable;
import java.util.Objects;

public class BillNumberSequenceId implements Serializable {
    private String clinicId;
    private int fy;

    public BillNumberSequenceId() {}

    public BillNumberSequenceId(String clinicId, int fy) {
        this.clinicId = clinicId;
        this.fy = fy;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillNumberSequenceId that)) return false;
        return fy == that.fy && Objects.equals(clinicId, that.clinicId);
    }

    @Override public int hashCode() { return Objects.hash(clinicId, fy); }
}
