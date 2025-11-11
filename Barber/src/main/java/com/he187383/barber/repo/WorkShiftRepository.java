//package com.he187383.barber.repo;
//
//import com.he187383.barber.entity.WorkShift;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//
//public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
//    // weekday: 0 = Mon … 6 = Sun
//    List<WorkShift> findByBarberIdAndWeekday(Long barberId, Integer weekday);
//
//    List<WorkShift> findByBarberId(Long barberId);
//}
