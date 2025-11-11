package com.he187383.barber.controller;

import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import com.he187383.barber.service.*;
import com.he187383.barber.util.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer/bookings")
@CrossOrigin(origins="*")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final UserRepository userRepo;
    private final BarberRepository barberRepo;
    private final ServiceItemRepository serviceRepo;
    private final WorkShiftRegRepository regRepo;
    private final TimeOffRepository timeOffRepo;
    private final BookingRepository bookingRepo;
    private final JwtService jwt;
    private final VnPayService vnp;

    public record CreateReq(String date, String startTime, Long barberId, Long serviceId, String method) {}
    public record CreateRes(Long bookingId, String payUrl) {}

    @PostMapping
    public ResponseEntity<CreateRes> create(@RequestBody CreateReq req, HttpServletRequest http) {
        var actor = requireCustomer(http);

        System.out.println("123123132132132132132");

        var svc = serviceRepo.findById(req.serviceId()).orElseThrow();
        var barber = barberRepo.findById(req.barberId()).orElseThrow();

        var day = java.time.LocalDate.parse(req.date());
        var start = parseTime(req.startTime());
        var end = start.plusMinutes(svc.getDurationMin());

        // 1) phải nằm trong 1 ca ACTIVE của thợ
        long cover = regRepo.countCovering(barber.getId(), day, start, end);
        if (cover == 0) throw new IllegalArgumentException("Khung giờ không nằm trong ca làm");

        // 2) không trùng time-off
        long off = timeOffRepo.countOverlap(barber.getId(), day, start, end);
        if (off > 0) throw new IllegalArgumentException("Thợ nghỉ trong khung giờ này");

        // 3) không trùng booking
        var startDt = LocalDateTime.of(day, start);
        var endDt   = LocalDateTime.of(day, end);
        long dup = bookingRepo.countOverlap(barber.getId(), startDt, endDt);
        if (dup > 0) throw new IllegalArgumentException("Khung giờ đã có lịch khác");

        // 4) tạo booking PENDING
        var user = userRepo.findById(actor.userId()).orElseThrow();
        var booking = bookingRepo.save(Booking.builder()
                .user(user).barber(barber).service(svc)
                .startDt(java.time.LocalDateTime.of(day, start))
                .endDt(java.time.LocalDateTime.of(day, end))
                .price(svc.getPrice())
                .status(Booking.Status.PENDING)
                .payMethod("VNPAY".equalsIgnoreCase(req.method()) ? Booking.PayMethod.VNPAY : Booking.PayMethod.CASH)
                .createdAt(java.time.LocalDateTime.now())
                .build());

        String payUrl = null;
        if (booking.getPayMethod() == Booking.PayMethod.VNPAY) {
            String txnRef = "BK" + booking.getId();
            booking.setPaymentRef(txnRef);
            bookingRepo.save(booking);
            payUrl = vnp.createPayUrl(clientIp(http), txnRef, booking.getPrice(),
                    "Booking " + booking.getId() + " - " + booking.getService().getName());
        }
        return ResponseEntity.ok(new CreateRes(booking.getId(), payUrl));
    }

    // xem lịch của tôi
    // trong CustomerBookingController (hoặc controller riêng)
    public record Mini(Long id, String name, String phone) {}
    public record MyBookingRes(
            Long id, String status, String payMethod, Integer price,
            java.time.LocalDateTime startDt, java.time.LocalDateTime endDt,
            Mini barber, Mini service) {}

    @GetMapping("/my")
    public ResponseEntity<List<MyBookingRes>> myBookings(HttpServletRequest http) {
        var actor = requireCustomer(http);
        var list = bookingRepo.findMyEager(actor.userId());

        var out = list.stream().map(b -> {
            var br = b.getBarber();
            var usr = br.getUser(); // đã fetch join, không còn proxy
            var svc = b.getService();
            return new MyBookingRes(
                    b.getId(),
                    b.getStatus().name(),
                    b.getPayMethod().name(),
                    b.getPrice(),
                    b.getStartDt(),
                    b.getEndDt(),
                    new Mini(br.getId(), br.getName(), usr != null ? usr.getPhone() : null),
                    new Mini(svc.getId(), svc.getName(), null)
            );
        }).toList();

        return ResponseEntity.ok(out);
    }


    // ===== helpers =====
    private record Actor(Long userId) {}
    private Actor requireCustomer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new IllegalStateException("UNAUTHORIZED");
        String token = h.substring(7);
        return new Actor(jwt.decodeUserId(token));
    }
    private java.time.LocalTime parseTime(String raw) {
        String s = raw==null? "": raw.trim().toLowerCase().replace('h', ':').replace('.', ':');
        if (s.matches("^\\d{1,2}$")) return java.time.LocalTime.of(Integer.parseInt(s), 0);
        if (s.matches("^\\d{3}$"))  return java.time.LocalTime.of(Integer.parseInt(s.substring(0,1)), Integer.parseInt(s.substring(1)));
        if (s.matches("^\\d{4}$"))  return java.time.LocalTime.of(Integer.parseInt(s.substring(0,2)), Integer.parseInt(s.substring(2)));
        if (s.matches("^\\d{1,2}:$")) s += "00";
        String[] p = s.split(":");
        return java.time.LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }
    private String clientIp(HttpServletRequest r){
        String x = r.getHeader("X-Forwarded-For");
        return (x!=null && !x.isBlank()) ? x.split(",")[0].trim() : r.getRemoteAddr();
    }
}

