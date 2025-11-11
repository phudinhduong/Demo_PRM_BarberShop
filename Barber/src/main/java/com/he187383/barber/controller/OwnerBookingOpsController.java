package com.he187383.barber.controller;

import com.he187383.barber.entity.Booking;
import com.he187383.barber.repo.BookingRepository;
import com.he187383.barber.util.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/bookings")
@RequiredArgsConstructor
public class OwnerBookingOpsController {
    private final BookingRepository bookingRepo;
    private final JwtService jwt;

    private void requireOwner(HttpServletRequest req){
        String h = req.getHeader("Authorization");
        if (h==null || !h.startsWith("Bearer ")) throw new IllegalStateException("UNAUTHORIZED");
        if (!"OWNER".equals(jwt.decodeRole(h.substring(7)))) throw new IllegalStateException("FORBIDDEN");
    }

    @PatchMapping("/{id}/cash-paid")
    public ResponseEntity<Booking> markCashPaid(@PathVariable Long id, HttpServletRequest req){
        requireOwner(req);
        var b = bookingRepo.findById(id).orElseThrow();
        if (b.getPayMethod() != Booking.PayMethod.CASH)
            throw new IllegalStateException("PAY_METHOD_NOT_CASH");
        if (b.getStatus() != Booking.Status.PENDING)
            throw new IllegalStateException("BOOKING_NOT_PENDING");
        b.setStatus(Booking.Status.PAID);
        return ResponseEntity.ok(bookingRepo.save(b));
    }

    public record Mini(Long id, String name, String phone) {}
    public record OwnerBookingRes(
            Long id, String status, String payMethod, Integer price,
            java.time.LocalDateTime startDt, java.time.LocalDateTime endDt,
            Mini service, Mini barber, Mini customer) {}

    // ====== LIST ALL (optional lọc ngày) ======
    @GetMapping
    public ResponseEntity<List<OwnerBookingRes>> listAll(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            HttpServletRequest req
    ) {
        requireOwner(req);
        java.time.LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        java.time.LocalDateTime toDt   = (to   != null) ? to.plusDays(1).atStartOfDay() : null;

        var list = bookingRepo.findAllEager(fromDt, toDt).stream().map(b -> {
            var svc = b.getService();
            var br  = b.getBarber();
            var brUser = (br != null) ? br.getUser() : null;
            var cus = b.getUser();
            return new OwnerBookingRes(
                    b.getId(),
                    b.getStatus().name(),
                    b.getPayMethod().name(),
                    b.getPrice(),
                    b.getStartDt(),
                    b.getEndDt(),
                    new Mini(svc != null ? svc.getId() : null, svc != null ? svc.getName() : null, null),
                    new Mini(br  != null ? br.getId()  : null, br  != null ? br.getName()  : null, brUser != null ? brUser.getPhone() : null),
                    new Mini(cus != null ? cus.getId() : null, cus != null ? cus.getName() : null, cus != null ? cus.getPhone() : null)
            );
        }).toList();

        return ResponseEntity.ok(list);
    }
    
}
