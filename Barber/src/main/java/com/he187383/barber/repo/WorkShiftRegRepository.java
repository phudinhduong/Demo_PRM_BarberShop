package com.he187383.barber.repo;


import com.he187383.barber.entity.WorkShiftReg;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WorkShiftRegRepository extends JpaRepository<WorkShiftReg, Long> {
    List<WorkShiftReg> findByWorkDateBetween(LocalDate from, LocalDate to);
    List<WorkShiftReg> findByBarberIdAndWorkDateBetween(Long barberId, LocalDate from, LocalDate to);
    List<WorkShiftReg> findByBarberIdAndWorkDate(Long barberId, LocalDate date);

    @Query("""
        select count(r) from WorkShiftReg r
         where r.barber.id = :barberId
           and r.workDate = :d
           and r.status = com.he187383.barber.entity.WorkShiftReg.Status.ACTIVE
           and r.startTime < :end
           and r.endTime   > :start
    """)
    long countOverlap(@Param("barberId") Long barberId,
                      @Param("d") LocalDate date,
                      @Param("start") LocalTime start,
                      @Param("end") LocalTime end);

    @Query("""
 select count(r) from WorkShiftReg r
  where r.barber.id=:barberId
    and r.workDate=:d
    and r.status = com.he187383.barber.entity.WorkShiftReg.Status.ACTIVE
    and r.startTime <= :start and r.endTime >= :end
""")
    long countCovering(@Param("barberId") Long barberId,
                       @Param("d") java.time.LocalDate d,
                       @Param("start") java.time.LocalTime start,
                       @Param("end") java.time.LocalTime end);

    List<WorkShiftReg> findByBarberIdAndWorkDateOrderByStartTime(Long barberId, java.time.LocalDate date);
}

