package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleBlockDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleIntervalDTO;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateRequest;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateResponse;
import com.MediHubAPI.model.enums.BlockType;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.SessionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceImplTest {

    private final ValidationServiceImpl validationService = new ValidationServiceImpl();

    @Test
    @DisplayName("Blocks fully inside a session interval are valid")
    void blocksInsideSessionIntervalAreAllowed() {
        ValidateRequest request = new ValidateRequest(
                ScheduleMode.DOCTOR_OVERRIDE,
                7L,
                null,
                LocalDate.of(2026, 1, 26),
                10,
                List.of(
                        day(
                                DayOfWeek.TUESDAY,
                                List.of(interval("08:00", "15:50")),
                                List.of(
                                        block("08:30", "08:50"),
                                        block("08:50", "08:55")
                                )
                        )
                )
        );

        ValidateResponse response = validationService.validate(request);

        assertThat(response.valid()).isTrue();
        assertThat(response.issues()).isEmpty();
    }

    @Test
    @DisplayName("Block that is not contained in any session interval is invalid")
    void blockOutsideSessionIntervalsIsRejected() {
        ValidateRequest request = new ValidateRequest(
                ScheduleMode.DOCTOR_OVERRIDE,
                7L,
                null,
                LocalDate.of(2026, 1, 26),
                10,
                List.of(
                        day(
                                DayOfWeek.TUESDAY,
                                List.of(interval("08:00", "15:50")),
                                List.of(block("07:50", "08:10"))
                        )
                )
        );

        ValidateResponse response = validationService.validate(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.issues())
                .extracting(issue -> issue.code() + "|" + issue.pointer())
                .contains("BLOCK_INTERVAL_CONFLICT|days[0].blocks[0]");
    }

    @Test
    @DisplayName("Block spanning a gap between sessions is invalid")
    void blockSpanningIntervalGapIsRejected() {
        ValidateRequest request = new ValidateRequest(
                ScheduleMode.DOCTOR_OVERRIDE,
                7L,
                null,
                LocalDate.of(2026, 1, 26),
                10,
                List.of(
                        day(
                                DayOfWeek.TUESDAY,
                                List.of(
                                        interval("08:00", "10:00"),
                                        interval("11:00", "13:00")
                                ),
                                List.of(block("09:50", "11:10"))
                        )
                )
        );

        ValidateResponse response = validationService.validate(request);

        assertThat(response.valid()).isFalse();
        assertThat(response.issues())
                .extracting(issue -> issue.code() + "|" + issue.pointer())
                .contains("BLOCK_INTERVAL_CONFLICT|days[0].blocks[0]");
    }

    private SessionScheduleDayPlanDTO day(
            DayOfWeek dayOfWeek,
            List<SessionScheduleIntervalDTO> intervals,
            List<SessionScheduleBlockDTO> blocks
    ) {
        return new SessionScheduleDayPlanDTO(dayOfWeek, false, intervals, blocks);
    }

    private SessionScheduleIntervalDTO interval(String start, String end) {
        return new SessionScheduleIntervalDTO(
                null,
                LocalTime.parse(start),
                LocalTime.parse(end),
                SessionType.OPD,
                1
        );
    }

    private SessionScheduleBlockDTO block(String start, String end) {
        return new SessionScheduleBlockDTO(
                null,
                BlockType.LUNCH,
                LocalTime.parse(start),
                LocalTime.parse(end),
                "test"
        );
    }
}
