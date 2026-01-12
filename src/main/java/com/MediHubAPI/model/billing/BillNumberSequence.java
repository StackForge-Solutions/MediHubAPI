package com.MediHubAPI.model.billing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Table(name = "bill_number_sequence")
@IdClass(BillNumberSequenceId.class)
public class BillNumberSequence {

    @Id
    @Column(name = "clinic_id", length = 64, nullable = false)
    private String clinicId;

    @Id
    @Column(name = "fy", nullable = false)
    private int fy;

    // ✅ DB column is next_val
    @Column(name = "next_val", nullable = false)
    private long nextVal;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    // getters/setters
}
