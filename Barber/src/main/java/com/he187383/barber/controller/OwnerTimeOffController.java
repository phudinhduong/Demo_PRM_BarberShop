package com.he187383.barber.controller;

import com.he187383.barber.dto.TimeOffDtos;
import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import com.he187383.barber.util.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/timeoffs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OwnerTimeOffController {

    private final TimeOffRepository repo;
    private final BarberRepository barberRepo;
    private final JwtService jwt;

    // ===== Guard =====
    private record Actor(Long userId, String role) {}
    private Actor requireOwner(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new IllegalStateException("UNAUTHORIZED");
        String token = h.substring(7);
        if (!"OWNER".equals(jwt.decodeRole(token))) throw new IllegalStateException("FORBIDDEN");
        return new Actor(jwt.decodeUserId(token), "OWNER");
    }


    @GetMapping
    public ResponseEntity<List<TimeOffDtos.DayGroup>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long barberId,
            HttpServletRequest http) {

        requireOwner(http);

        List<TimeOff> raw = (barberId != null)
                ? repo.findByBarberIdAndDateBetween(barberId, from, to)
                : repo.findByDateBetween(from, to);

        // group theo date
        Map<LocalDate, List<TimeOffDtos.DayItem>> map = new TreeMap<>();
        for (TimeOff t : raw) {
            var b = t.getBarber();
            var u = b.getUser();
            String phone = (u != null && u.getPhone() != null) ? u.getPhone() : "";
            TimeOffDtos.DayItem item = new TimeOffDtos.DayItem(
                    t.getId(),
                    t.getAllDay() ? "ALL_DAY" : "SLOT",
                    t.getAllDay() ? null : safeTime(t.getStartTime()),
                    t.getAllDay() ? null : safeTime(t.getEndTime()),
                    t.getReason(),
                    new TimeOffDtos.MiniBarber(b.getId(), b.getName(), phone)
            );
            map.computeIfAbsent(t.getDate(), k -> new ArrayList<>()).add(item);
        }
        // sort items mỗi ngày: ALL_DAY trước, rồi theo start
        List<TimeOffDtos.DayGroup> result = map.entrySet().stream().map(e -> {
            List<TimeOffDtos.DayItem> items = e.getValue().stream()
                    .sorted(Comparator
                            .comparing(TimeOffDtos.DayItem::type)
                            .thenComparing(i -> zeroIfNull(i.start())))
                    .collect(Collectors.toList());
            return new TimeOffDtos.DayGroup(e.getKey(), items);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<TimeOffDtos.CreateRes> create(@RequestBody TimeOffDtos.CreateReq req, HttpServletRequest http) {
        Actor actor = requireOwner(http);

        if (req.barberIds() == null || req.barberIds().isEmpty())
            throw new IllegalArgumentException("barberIds required");
        if (req.allDay() == null)
            throw new IllegalArgumentException("allDay required");
        LocalDate d = LocalDate.parse(req.date());

        List<Long> created = new ArrayList<>();
        List<TimeOffDtos.Skip> skipped = new ArrayList<>();

        for (Long barberId : new LinkedHashSet<>(req.barberIds())) {
            Barber b = barberRepo.findById(barberId).orElse(null);
            if (b == null) { skipped.add(new TimeOffDtos.Skip(barberId, "BARBER_NOT_FOUND")); continue; }

            if (Boolean.TRUE.equals(req.allDay())) {
                // chống trùng: nếu đã có bất kỳ timeoff trong ngày
                long ov = repo.countOverlap(barberId, d, LocalTime.MIN, LocalTime.MAX);
                if (ov > 0) { skipped.add(new TimeOffDtos.Skip(barberId, "OVERLAP_EXISTED")); continue; }

                TimeOff tf = repo.save(TimeOff.builder()
                        .barber(b).date(d).allDay(true)
                        .reason(req.reason())
                        .createdByRole(actor.role()).createdByUserId(actor.userId())
                        .createdAt(LocalDateTime.now())
                        .build());
                created.add(tf.getId());
            } else {
                if (req.slots() == null || req.slots().isEmpty()) {
                    skipped.add(new TimeOffDtos.Skip(barberId, "SLOTS_EMPTY")); continue;
                }
                for (TimeOffDtos.SlotReq s : req.slots()) {
                    LocalTime st = parseTime(s.start());
                    LocalTime en = parseTime(s.end());
                    if (!st.isBefore(en)) { skipped.add(new TimeOffDtos.Skip(barberId, "INVALID_RANGE:"+s.start()+"-"+s.end())); continue; }
                    long ov = repo.countOverlap(barberId, d, st, en);
                    if (ov > 0) { skipped.add(new TimeOffDtos.Skip(barberId, "OVERLAP_EXISTED:"+s.start()+"-"+s.end())); continue; }

                    TimeOff tf = repo.save(TimeOff.builder()
                            .barber(b).date(d).allDay(false)
                            .startTime(st).endTime(en)
                            .reason(req.reason())
                            .createdByRole(actor.role()).createdByUserId(actor.userId())
                            .createdAt(LocalDateTime.now())
                            .build());
                    created.add(tf.getId());
                }
            }
        }
        return ResponseEntity.ok(new TimeOffDtos.CreateRes(created, skipped));
    }

    // ===== 3) XOÁ =====
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest http) {
        requireOwner(http);
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ===== helpers =====
    private String safeTime(LocalTime t){ return t==null? null : t.toString(); }
    private String zeroIfNull(String s){ return s==null? "00:00" : s; }

    private LocalTime parseTime(String raw) {
        String s = raw == null ? "" : raw.trim().toLowerCase().replace('h', ':').replace('.', ':');
        if (s.matches("^\\d{1,2}$")) return LocalTime.of(Integer.parseInt(s), 0);
        if (s.matches("^\\d{3}$"))  return LocalTime.of(Integer.parseInt(s.substring(0,1)), Integer.parseInt(s.substring(1)));
        if (s.matches("^\\d{4}$"))  return LocalTime.of(Integer.parseInt(s.substring(0,2)), Integer.parseInt(s.substring(2)));
        if (s.matches("^\\d{1,2}:$")) s = s + "00";
        if (s.matches("^\\d{1,2}:\\d{1,2}$")) {
            String[] p = s.split(":"); int h = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]);
            if (h<0||h>23||m<0||m>59) throw new IllegalArgumentException("time out of range");
            return LocalTime.of(h, m);
        }
        throw new IllegalArgumentException("Invalid time: " + raw);
    }
}
