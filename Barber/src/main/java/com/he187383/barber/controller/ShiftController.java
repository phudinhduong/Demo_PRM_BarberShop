package com.he187383.barber.controller;

import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ShiftController {

    private final BarberRepository barberRepo;
    private final WorkShiftRegRepository regRepo;
    private final TimeOffRepository timeOffRepo;

    public record MiniBarber(Long id, String name, String phone) {}
    public record Bucket(String start, String end, List<MiniBarber> barbers) {}
    public record DayBuckets(Bucket morning, Bucket afternoon, Bucket evening) {}

    // Sáng 08:00–12:00, Chiều 13:30–17:00, Tối 18:00–21:00 (khớp UI client)
    private static final LocalTime M_START = LocalTime.of(8,0),  M_END = LocalTime.of(12,0);
    private static final LocalTime A_START = LocalTime.of(13,30), A_END = LocalTime.of(17,0);
    private static final LocalTime E_START = LocalTime.of(18,0),  E_END = LocalTime.of(21,0);

    @GetMapping("/day")
    public ResponseEntity<DayBuckets> day(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long barberId
    ) {
        List<Barber> scope = (barberId != null)
                ? barberRepo.findById(barberId).map(List::of).orElse(List.of())
                : barberRepo.findByIsActiveTrue();

        Bucket m = bucket(date, M_START, M_END, scope);
        Bucket a = bucket(date, A_START, A_END, scope);
        Bucket e = bucket(date, E_START, E_END, scope);

        return ResponseEntity.ok(new DayBuckets(m, a, e));
    }

    private Bucket bucket(LocalDate d, LocalTime start, LocalTime end, List<Barber> scope) {
        List<MiniBarber> ok = new ArrayList<>();
        for (Barber b : scope) {
            if (b.getIsActive() == null || !b.getIsActive()) continue;

            long cover = regRepo.countCovering(b.getId(), d, start, end);
            if (cover == 0) continue;

            long off = timeOffRepo.countOverlap(b.getId(), d, start, end);
            if (off > 0) continue;

            String phone = (b.getUser() != null) ? b.getUser().getPhone() : null;
            ok.add(new MiniBarber(b.getId(), b.getName(), phone));
        }
        return new Bucket(start.toString(), end.toString(), ok);
    }
}
