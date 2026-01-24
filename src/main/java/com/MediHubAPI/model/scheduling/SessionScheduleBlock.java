package com.MediHubAPI.model.scheduling;

import com.MediHubAPI.model.enums.BlockType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "session_schedule_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionScheduleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private SessionScheduleDay day;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 30)
    private BlockType blockType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason", length = 250)
    private String reason;
}
