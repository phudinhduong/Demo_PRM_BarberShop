package com.he187383.barber.dto;

import java.time.LocalDate;
import java.util.List;

public class WorkShiftDtos {

    // ===== DTOs view =====
    public record DayBuckets(LocalDate date, Bucket morning, Bucket afternoon, Bucket evening) {}
    public record Bucket(String label, String start, String end, List<MiniBarber> barbers) {}
    public record MiniBarber(Long id, String name, String phone) {}

    public record RegisterRes(List<Long> createdIds, List<Skip> skipped) {}
    public record Skip(Long barberId, String reason) {}
}
