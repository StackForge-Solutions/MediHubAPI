package com.MediHubAPI.model.scheduling.session;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_schedule_days",
        uniqueConstraints = @UniqueConstraint(name = "uk_schedule_day", columnNames = {"schedule_id", "day_of_week"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionScheduleDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private SessionSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_day_off", nullable = false)
    private boolean dayOff;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionScheduleInterval> intervals = new ArrayList<>();

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionScheduleBlock> blocks = new ArrayList<>();

    public void setIntervalsReplace(List<SessionScheduleInterval> newIntervals) {
        this.intervals.clear();
        if (newIntervals == null) return;
        for (SessionScheduleInterval i : newIntervals) {
            i.setDay(this);
            this.intervals.add(i);
        }
    }

    public void setBlocksReplace(List<SessionScheduleBlock> newBlocks) {
        this.blocks.clear();
        if (newBlocks == null) return;
        for (SessionScheduleBlock b : newBlocks) {
            b.setDay(this);
            this.blocks.add(b);
        }
    }
}
