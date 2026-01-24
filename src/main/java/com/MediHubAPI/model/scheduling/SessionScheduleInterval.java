package com.MediHubAPI.model.scheduling;

import com.MediHubAPI.model.enums.SessionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "session_schedule_intervals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionScheduleInterval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private SessionScheduleDay day;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    // Optional capacity per slot/session (for future)
    @Column(name = "capacity", nullable = false)
    private Integer capacity;
}
