package com.he187383.barber.config;

import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.*;


@Component
@Order(5)
@RequiredArgsConstructor
public class DataSeed implements CommandLineRunner {

    private final UserRepository userRepo;
    private final BarberRepository barberRepo;
    private final ServiceItemRepository serviceRepo;
    private final WorkShiftRegRepository regRepo;
    private final WorkShiftLogRepository logRepo;
    private final TimeOffRepository timeOffRepo;
    private final PasswordEncoder enc;

    @Override
    public void run(String... args) {
        var owner = seedOwner("Owner", "ddinhphu2004@gmail.com", "0123456789");
        var customer = seedCustomer("Customer", "phudinh193@gmail.com", "0987654321");

        var b1 = seedBarber("Thợ A", "barber1@gmail.com", "10 năm kinh nghiệm", "0901111111");
        var b2 = seedBarber("Thợ B", "barber2@gmail.com", "Khéo tay, tỉ mỉ",        "0902222222");

        seedServices(
                new ServiceSeed("Cắt tóc nam", 30, 50000),
                new ServiceSeed("Gội & massage", 20, 30000),
                new ServiceSeed("Combo cắt + gội", 50, 80000)
        );

        // Seed ca làm 4 tuần (T2–T6: 09:00–12:00, 13:30–17:00)
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        seedShiftsForAll(start, 4, owner.getId());

        // Time-off mẫu: ngày mai B1 nghỉ 14:00–15:30, CN tuần này B2 nghỉ cả ngày
        seedSampleTimeOffs(b1.getId(), b2.getId(), owner.getId());

        System.out.println("== Seeding DONE ==");
        System.out.println("Owner     : ddinhphu2004@gmail.com / 123456");
        System.out.println("Customer  : phudinh193@gmail.com   / 123456");
        System.out.println("Barber A  : barber1@gmail.com      / 123456");
        System.out.println("Barber B  : barber2@gmail.com      / 123456");
    }

    // ---------- Users ----------
    private User seedOwner(String name, String email, String phone) {
        return userRepo.findByEmail(email).orElseGet(() -> {
            var u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(enc.encode("123456"));
            u.setRole(User.Role.OWNER);
            u.setEmailVerifiedAt(LocalDateTime.now());
            System.out.println("== Seeded OWNER: " + email + " / 123456 ==");
            return userRepo.save(u);
        });
    }

    private User seedCustomer(String name, String email, String phone) {
        return userRepo.findByEmail(email).orElseGet(() -> {
            var u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(enc.encode("123456"));
            u.setRole(User.Role.CUSTOMER);
            u.setEmailVerifiedAt(LocalDateTime.now());
            System.out.println("== Seeded CUSTOMER: " + email + " / 123456 ==");
            return userRepo.save(u);
        });
    }

    private Barber seedBarber(String name, String email, String bio, String phone) {
        var user = userRepo.findByEmail(email).orElseGet(() -> {
            var u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(enc.encode("123456"));
            u.setRole(User.Role.BARBER);
            u.setEmailVerifiedAt(LocalDateTime.now());
            return userRepo.save(u);
        });

        return barberRepo.findByUserId(user.getId()).orElseGet(() -> {
            var b = Barber.builder()
                    .user(user)
                    .name(name)
                    .bio(bio)
                    .avatarUrl(null)
                    .isActive(true)
                    .build();
            System.out.println("== Seeded BARBER: " + name + " / " + email + " / 123456 ==");
            return barberRepo.save(b);
        });
    }

    // ---------- Services ----------
    private record ServiceSeed(String name, int durationMin, int price) {}
    private void seedServices(ServiceSeed... items) {
        var existed = new LinkedHashSet<>(serviceRepo.findAll()
                .stream().map(ServiceItem::getName).toList());
        for (var it : items) {
            if (existed.contains(it.name)) continue;
            var s = new ServiceItem();
            s.setName(it.name);
            s.setDurationMin(it.durationMin);
            s.setPrice(it.price);
            s.setIsActive(true);
            serviceRepo.save(s);
            System.out.println("== Seeded SERVICE: " + it.name + " (" + it.durationMin + "m, " + it.price + "đ)");
        }
    }

    // ---------- Shifts (4 tuần) ----------
    private void seedShiftsForAll(LocalDate weekStart, int weeks, Long ownerId) {
        List<Barber> barbers = barberRepo.findAll();
        if (barbers.isEmpty()) return;

        LocalDate from = weekStart;
        LocalDate to = weekStart.plusWeeks(weeks).minusDays(1);
        Set<DayOfWeek> workingDays = EnumSet.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );

        for (Barber b : barbers) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (!workingDays.contains(d.getDayOfWeek())) continue;
                createShiftIfFree(b, d, LocalTime.of(9,0),  LocalTime.of(12,0),  ownerId);
                createShiftIfFree(b, d, LocalTime.of(13,30),LocalTime.of(17,0), ownerId);
            }
        }
        System.out.println("== Seeded shifts for " + barbers.size() + " barber(s): " + from + " .. " + to);
    }

    private void createShiftIfFree(Barber b, LocalDate date, LocalTime start, LocalTime end, Long ownerId) {
        long overlaps = regRepo.countOverlap(b.getId(), date, start, end);
        if (overlaps > 0) return;
        var reg = regRepo.save(WorkShiftReg.builder()
                .barber(b)
                .workDate(date)
                .startTime(start)
                .endTime(end)
                .status(WorkShiftReg.Status.ACTIVE)
                .createdByRole("OWNER")
                .createdByUserId(ownerId)
                .createdAt(LocalDateTime.now())
                .build());
        logRepo.save(WorkShiftLog.builder()
                .shift(reg)
                .action("CREATED")
                .actorRole("OWNER")
                .actorUserId(ownerId)
                .at(LocalDateTime.now())
                .build());
    }

    // ---------- Time-off mẫu ----------
    private void seedSampleTimeOffs(Long barberAId, Long barberBId, Long ownerId) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // B1 nghỉ 14:00–15:30 ngày mai
        if (timeOffRepo.countOverlap(barberAId, tomorrow, LocalTime.of(14,0), LocalTime.of(15,30)) == 0) {
            timeOffRepo.save(TimeOff.builder()
                    .barber(barberRepo.getReferenceById(barberAId))
                    .date(tomorrow)
                    .allDay(false)
                    .startTime(LocalTime.of(14,0))
                    .endTime(LocalTime.of(15,30))
                    .reason("Việc riêng")
                    .createdByRole("OWNER")
                    .createdByUserId(ownerId)
                    .createdAt(LocalDateTime.now())
                    .build());
            System.out.println("== Seeded TIMEOFF: BarberA " + tomorrow + " 14:00–15:30");
        }
        // B2 nghỉ cả ngày Chủ nhật tuần này
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);
        if (timeOffRepo.countOverlap(barberBId, sunday, LocalTime.MIN, LocalTime.MAX) == 0) {
            timeOffRepo.save(TimeOff.builder()
                    .barber(barberRepo.getReferenceById(barberBId))
                    .date(sunday)
                    .allDay(true)
                    .reason("Nghỉ lễ")
                    .createdByRole("OWNER")
                    .createdByUserId(ownerId)
                    .createdAt(LocalDateTime.now())
                    .build());
            System.out.println("== Seeded TIMEOFF: BarberB " + sunday + " ALL-DAY");
        }
    }
}
