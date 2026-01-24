package com.MediHubAPI.repository.scheduling.session;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;
import com.MediHubAPI.model.scheduling.SessionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SessionScheduleRepository extends JpaRepository<SessionSchedule, Long> {

    Optional<SessionSchedule> findByDoctorIdAndWeekStartDateAndModeAndStatusNot(
            Long doctorId, LocalDate weekStartDate, ScheduleMode mode, ScheduleStatus status);

    List<SessionSchedule> findByDoctorIdAndWeekStartDate(Long doctorId, LocalDate weekStartDate);

    List<SessionSchedule> findByModeAndWeekStartDate(ScheduleMode mode, LocalDate weekStartDate);

    List<SessionSchedule> findByModeAndDoctorIdAndWeekStartDate(ScheduleMode mode, Long doctorId, LocalDate weekStartDate);
}
