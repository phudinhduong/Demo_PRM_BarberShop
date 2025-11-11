package com.he187383.barber.dto;

import java.time.LocalDate;
import java.util.List;

public class TimeOffDtos {

    public record DayItem(Long id, String type, String start, String end, String reason,
                          MiniBarber barber) {}
    public record MiniBarber(Long id, String name, String phone) {}
    public record DayGroup(LocalDate date, List<DayItem> items) {}


    // ===== 2)
    public record SlotReq(String start, String end) {}
    public record CreateReq(
            String date,                 // yyyy-MM-dd
            Boolean allDay,              // true = nghỉ cả ngày
            List<SlotReq> slots,         // nếu allDay=false: danh sách khung giờ
            List<Long> barberIds,        // danh sách thợ áp dụng
            String reason
    ) {}
    public record CreateRes(List<Long> createdIds, List<Skip> skipped) {}
    public record Skip(Long barberId, String detail) {}

}
