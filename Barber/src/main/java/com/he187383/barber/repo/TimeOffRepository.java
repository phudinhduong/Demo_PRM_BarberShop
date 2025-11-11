package com.he187383.barber.repo;

import com.he187383.barber.entity.TimeOff;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TimeOffRepository extends JpaRepository<TimeOff, Long> {

    List<TimeOff> findByDateBetween(LocalDate from, LocalDate to);
    List<TimeOff> findByBarberIdAndDateBetween(Long barberId, LocalDate from, LocalDate to);

    @Query("""
        select count(t) from TimeOff t
         where t.barber.id = :barberId
           and t.date = :d
           and (
                t.allDay = true
             or (:start < t.endTime and :end > t.startTime)
           )
    """)
    long countOverlap(
            @Param("barberId") Long barberId,
            @Param("d") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end
    );

    List<TimeOff> findByBarberIdAndDateOrderByAllDayDesc(Long barberId, java.time.LocalDate date);
}
