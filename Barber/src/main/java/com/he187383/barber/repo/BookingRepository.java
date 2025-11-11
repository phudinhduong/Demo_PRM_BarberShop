package com.he187383.barber.repo;

import com.he187383.barber.entity.Booking;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
        select count(b) from Booking b
         where b.barber.id = :barberId
           and b.status in (com.he187383.barber.entity.Booking.Status.PENDING,
                            com.he187383.barber.entity.Booking.Status.PAID)
           and b.startDt < :endDt and b.endDt > :startDt
    """)
    long countOverlap(@Param("barberId") Long barberId,
                      @Param("startDt") LocalDateTime startDt,
                      @Param("endDt") LocalDateTime endDt);

    List<Booking> findByUserIdOrderByStartDtDesc(Long userId);

    @Query("""
  select b from Booking b
   join fetch b.barber br
   join fetch b.service s
   left join fetch br.user u
  where b.user.id = :uid
  order by b.startDt desc
""")
    List<Booking> findMyEager(@Param("uid") Long uid);

    @Query("""
  select b from Booking b
   join fetch b.user u
   join fetch b.service s
  where b.barber.id = :bid
    and b.startDt >= :from and b.startDt < :to
  order by b.startDt asc
""")
    List<Booking> findBarberBookingsOnDay(@Param("bid") Long barberId,
                                          @Param("from") java.time.LocalDateTime from,
                                          @Param("to")   java.time.LocalDateTime to);
}
