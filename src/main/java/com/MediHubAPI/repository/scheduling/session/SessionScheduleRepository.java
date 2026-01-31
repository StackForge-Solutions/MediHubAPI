package com.MediHubAPI.repository.scheduling.session;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;

public interface SessionScheduleRepository extends JpaRepository<SessionSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    Optional<SessionSchedule> findByDoctorIdAndWeekStartDateAndModeAndStatusNot(
            Long doctorId, LocalDate weekStartDate, ScheduleMode mode, ScheduleStatus status);

    List<SessionSchedule> findByDoctorIdAndWeekStartDate(Long doctorId, LocalDate weekStartDate);

    List<SessionSchedule> findByModeAndWeekStartDate(ScheduleMode mode, LocalDate weekStartDate);

    List<SessionSchedule> findByModeAndDoctorIdAndWeekStartDate(
            ScheduleMode mode, Long doctorId, LocalDate weekStartDate);

    // FIX A: Only fetch "days" to avoid multiple-bag fetch error
    @EntityGraph(attributePaths = { "days" })
    List<SessionSchedule> findWithChildrenByModeAndWeekStartDate(
            ScheduleMode mode, LocalDate weekStartDate);

    // FIX A: Only fetch "days" to avoid multiple-bag fetch error
    @EntityGraph(attributePaths = { "days" })
    List<SessionSchedule> findWithChildrenByModeAndDoctorIdAndWeekStartDate(
            ScheduleMode mode, Long doctorId, LocalDate weekStartDate);
}
