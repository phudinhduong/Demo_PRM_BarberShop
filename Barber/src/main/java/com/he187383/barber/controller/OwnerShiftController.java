package com.he187383.barber.controller;

import com.he187383.barber.dto.WorkShiftDtos;
import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import com.he187383.barber.util.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/shifts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OwnerShiftController {

    private final WorkShiftRegRepository regRepo;
    private final WorkShiftLogRepository logRepo;
    private final BarberRepository barberRepo;
    private final JwtService jwt;

    // ===== Guard =====
    private record Actor(Long userId, String role) {}
    private Actor requireOwner(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new IllegalStateException("UNAUTHORIZED");
        String token = h.substring(7);
        String role = jwt.decodeRole(token);
        if (!"OWNER".equals(role)) throw new IllegalStateException("FORBIDDEN");
        return new Actor(jwt.decodeUserId(token), role);
    }

    // ===== View: chọn ngày → 3 khung (Sáng/Chiều/Tối) kèm thợ (tên, sđt) =====
    @GetMapping("/day")
    public ResponseEntity<WorkShiftDtos.DayBuckets> viewDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest http) {

        requireOwner(http);

        var regs = regRepo.findByWorkDateBetween(date, date)
                .stream().filter(r -> r.getStatus() == WorkShiftReg.Status.ACTIVE).toList();

        var morning = collect(regs, Slot.MORNING);
        var afternoon = collect(regs, Slot.AFTERNOON);
        var evening = collect(regs, Slot.EVENING);

        return ResponseEntity.ok(new WorkShiftDtos.DayBuckets(
                date,
                new WorkShiftDtos.Bucket("Sáng", Slot.MORNING.start().toString(), Slot.MORNING.end().toString(), morning),
                new WorkShiftDtos.Bucket("Chiều", Slot.AFTERNOON.start().toString(), Slot.AFTERNOON.end().toString(), afternoon),
                new WorkShiftDtos.Bucket("Tối", Slot.EVENING.start().toString(), Slot.EVENING.end().toString(), evening)
        ));
    }

    // ===== Thêm: chọn ngày + khung giờ + nhiều barber =====
    public record RegisterReq(
            @NotBlank String date,          // yyyy-MM-dd
            @NotBlank String slot,          // MORNING | AFTERNOON | EVENING
            @NotNull  List<Long> barberIds  // danh sách barber
    ) {}

    @PostMapping("/register")
    public ResponseEntity<WorkShiftDtos.RegisterRes> register(@RequestBody RegisterReq req, HttpServletRequest http) {
        Actor actor = requireOwner(http);

        LocalDate d = LocalDate.parse(req.date());
        Slot slot = Slot.of(req.slot());
        List<Long> createdIds = new ArrayList<>();
        List<WorkShiftDtos.Skip> skipped = new ArrayList<>();

        for (Long barberId : new LinkedHashSet<>(req.barberIds())) { // unique
            var b = barberRepo.findById(barberId).orElse(null);
            if (b == null) { skipped.add(new WorkShiftDtos.Skip(barberId, "BARBER_NOT_FOUND")); continue; }

            // chống trùng trong khung đã chọn
            long overlaps = regRepo.countOverlap(barberId, d, slot.start(), slot.end());
            if (overlaps > 0) { skipped.add(new WorkShiftDtos.Skip(barberId, "OVERLAP_EXISTED")); continue; }

            var reg = regRepo.save(WorkShiftReg.builder()
                    .barber(b)
                    .workDate(d)
                    .startTime(slot.start())
                    .endTime(slot.end())
                    .status(WorkShiftReg.Status.ACTIVE)
                    .createdByRole("OWNER")
                    .createdByUserId(actor.userId())
                    .createdAt(LocalDateTime.now())
                    .build());
            createdIds.add(reg.getId());

            logRepo.save(WorkShiftLog.builder()
                    .shift(reg)
                    .action("CREATED")
                    .actorRole("OWNER")
                    .actorUserId(actor.userId())
                    .at(LocalDateTime.now())
                    .build());
        }
        return ResponseEntity.ok(new WorkShiftDtos.RegisterRes(createdIds, skipped));
    }

    // ===== Huỷ: cần lý do =====
    public record CancelReq(String reason) {}

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<WorkShiftReg> cancel(@PathVariable Long id,
                                               @RequestBody(required = false) CancelReq req,
                                               HttpServletRequest http) {
        Actor actor = requireOwner(http);

        var reg = regRepo.findById(id).orElseThrow();
        if (reg.getStatus() == WorkShiftReg.Status.CANCELLED) return ResponseEntity.ok(reg);

        reg.setStatus(WorkShiftReg.Status.CANCELLED);
        reg.setCancelByRole("OWNER");
        reg.setCancelByUserId(actor.userId());
        reg.setCancelReason(req != null ? req.reason() : null);
        reg.setCancelAt(LocalDateTime.now());
        var saved = regRepo.save(reg);

        logRepo.save(WorkShiftLog.builder()
                .shift(saved)
                .action("CANCELLED")
                .actorRole("OWNER")
                .actorUserId(actor.userId())
                .reason(saved.getCancelReason())
                .at(LocalDateTime.now())
                .build());

        return ResponseEntity.ok(saved);
    }

    // ===== Helper: gom thợ theo khung =====
    private List<WorkShiftDtos.MiniBarber> collect(List<WorkShiftReg> regs, Slot slot) {
        Map<Long, WorkShiftDtos.MiniBarber> map = new LinkedHashMap<>();
        for (var r : regs) {
            if (overlap(r.getStartTime(), r.getEndTime(), slot.start(), slot.end())) {
                var b = r.getBarber();
                User u = b.getUser();
                String phone = (u != null && u.getPhone() != null) ? u.getPhone() : "";
                map.putIfAbsent(b.getId(), new WorkShiftDtos.MiniBarber(b.getId(), b.getName(), phone));
            }
        }
        return map.values().stream()
                .sorted(Comparator.comparing(WorkShiftDtos.MiniBarber::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
    private boolean overlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    // ===== Fixed slots =====
    private enum Slot {
        MORNING(LocalTime.of(8,0),  LocalTime.of(12,0)),
        AFTERNOON(LocalTime.of(13,30), LocalTime.of(17,0)),
        EVENING(LocalTime.of(18,0), LocalTime.of(21,0));

        private final LocalTime start, end;
        Slot(LocalTime s, LocalTime e){ this.start=s; this.end=e; }
        public LocalTime start(){ return start; }
        public LocalTime end(){ return end; }

        public static Slot of(String s){
            if (s == null) throw new IllegalArgumentException("slot required");
            return Slot.valueOf(s.trim().toUpperCase());
        }
    }
}

