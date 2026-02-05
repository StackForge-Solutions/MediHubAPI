package com.MediHubAPI.repository;

import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long>, JpaSpecificationExecutor<Slot> {

    /**
     * Fetch all slots for a doctor on a specific date.
     */
    List<Slot> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    /**
     * Fetch all slots for a doctor on a specific date within a time range.
     */
    List<Slot> findByDoctorIdAndDateAndStartTimeBetween(Long doctorId, LocalDate date,
                                                        LocalTime startTime, LocalTime endTime);

    /**
     * Fetch slots by doctor, date, and status (e.g., AVAILABLE, BLOCKED, etc.).
     */
    List<Slot> findByDoctorIdAndDateAndStatusIn(Long doctorId, LocalDate date, List<SlotStatus> statuses);

    /**
     * Fetch slots for a doctor between two dates that match specific statuses.
     */
    List<Slot> findByDoctorIdAndDateBetweenAndStatusInOrderByDateAscStartTimeAsc(Long doctorId, LocalDate startDate,
                                                                                LocalDate endDate, List<SlotStatus> statuses);

    boolean existsByDoctorIdAndDateAndStartTimeAndEndTime(Long id, LocalDate date, LocalTime current, LocalTime slotEnd);

    Optional<Slot> findByDoctorIdAndStartTimeAndDate(Long doctorId, LocalTime slotTime, LocalDate appointmentDate);

}
