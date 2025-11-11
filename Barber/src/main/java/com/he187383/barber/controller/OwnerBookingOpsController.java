package com.he187383.barber.controller;

import com.he187383.barber.entity.Booking;
import com.he187383.barber.repo.BookingRepository;
import com.he187383.barber.util.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
