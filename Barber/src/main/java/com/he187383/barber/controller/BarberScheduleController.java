package com.he187383.barber.controller;


import com.he187383.barber.repo.*;
import com.he187383.barber.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/barber")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BarberScheduleController {
    private final BarberRepository barberRepo;
    private final WorkShiftRegRepository regRepo;
    private final TimeOffRepository timeOffRepo;
    private final BookingRepository bookingRepo;
    private final JwtService jwt;

    // DTOs
    public record ShiftDto(Long id, String start, String end, String status) {}
    public record OffDto(Long id, boolean allDay, String start, String end, String reason) {}
    public record BookingDto(Long id, String start, String end,
                             String service, Integer price,
                             String status, String payMethod,
                             Long customerId, String customerName, String customerPhone) {}
    public record DayRes(List<ShiftDto> shifts, List<OffDto> timeOffs, List<BookingDto> bookings) {}

    @GetMapping("/day")
    public ResponseEntity<DayRes> myDay(@RequestParam
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        HttpServletRequest req) {
        Actor me = requireBarber(req);

        // Ca làm
        var shifts = regRepo.findByBarberIdAndWorkDateOrderByStartTime(me.userBarberId(), date)
                .stream().map(s -> new ShiftDto(
                        s.getId(),
                        s.getStartTime().toString(),
                        s.getEndTime().toString(),
                        s.getStatus().name()
                )).toList();

        // Time-off
        var offs = timeOffRepo.findByBarberIdAndDateOrderByAllDayDesc(me.userBarberId(), date)
                .stream().map(o -> new OffDto(
                        o.getId(),
                        Boolean.TRUE.equals(o.getAllDay()),
                        o.getStartTime()!=null ? o.getStartTime().toString() : null,
                        o.getEndTime()!=null ? o.getEndTime().toString() : null,
                        o.getReason()
                )).toList();

        // Bookings trong ngày
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.plusDays(1).atStartOfDay();
        var bookings = bookingRepo.findBarberBookingsOnDay(me.userBarberId(), from, to)
                .stream().map(b -> new BookingDto(
                        b.getId(),
                        b.getStartDt().toString(),
                        b.getEndDt().toString(),
                        b.getService().getName(),
                        b.getPrice(),
                        b.getStatus().name(),
                        b.getPayMethod().name(),
                        b.getUser().getId(),
                        b.getUser().getName(),
                        b.getUser().getPhone()
                )).toList();

        return ResponseEntity.ok(new DayRes(shifts, offs, bookings));
    }

    // ===== auth helper =====
    private record Actor(Long userId, Long userBarberId) {}
    private Actor requireBarber(HttpServletRequest req){
        String h = req.getHeader("Authorization");
        if (h==null || !h.startsWith("Bearer ")) throw new IllegalStateException("UNAUTHORIZED");
        String token = h.substring(7);
        String role = jwt.decodeRole(token);
        if (!"BARBER".equals(role)) throw new IllegalStateException("FORBIDDEN");
        Long uid = jwt.decodeUserId(token);
        // map User -> Barber
        // giả sử có method trong BarberRepository: findByUserId(...)
        // nếu không, tự bơm repo và tra ở đây.
        // (Giữ gọn, chỉ trả về id đã gán sẵn khi seed)
        Long barberId = barberRepo.findByUserId(uid).orElseThrow().getId();
        return new Actor(uid, barberId);
    }
}
