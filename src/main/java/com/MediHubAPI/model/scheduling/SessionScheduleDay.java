package com.MediHubAPI.model.scheduling;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

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
