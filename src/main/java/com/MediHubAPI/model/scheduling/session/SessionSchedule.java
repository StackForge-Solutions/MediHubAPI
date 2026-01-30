package com.MediHubAPI.model.scheduling.session;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;

@Entity
// Enforce one active schedule per doctor/week/mode at the database layer.
@Table(
        name = "session_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_session_schedule_doctor_week_mode",
                columnNames = { "doctor_id", "week_start_date", "mode" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Matches UI concept: GLOBAL_TEMPLATE vs DOCTOR_OVERRIDE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ScheduleMode mode;

    // Doctor target (reuses existing User doctor id via adapter; no FK here to avoid coupling)
    @Column(name = "doctor_id")
    private Long doctorId;

    // Optional department label/id (reuse your Specialization.department or real Department via adapter)
    @Column(name = "department_id")
    private Long departmentId;

    // Week start date (ISO) e.g., Monday of that week
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    // Slot duration used during slot slicing
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleStatus status;

    // Minimal lock flags for UI/backend concurrency coordination
    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "locked_reason", length = 200)
    private String lockedReason;

    @Version
    @Column(nullable = false)
    private Long version;

    // Audit (optional integration: ActorProvider)
    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionScheduleDay> days = new ArrayList<>();

    public void touchForCreate(String actor) {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actor;
        this.updatedBy = actor;
    }

    public void touchForUpdate(String actor) {
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public void setDaysReplace(List<SessionScheduleDay> newDays) {
        this.days.clear();
        if (newDays == null) return;
        for (SessionScheduleDay d : newDays) {
            d.setSchedule(this);
            this.days.add(d);
        }
    }
}
