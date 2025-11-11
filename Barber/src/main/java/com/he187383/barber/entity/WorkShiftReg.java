package com.he187383.barber.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Table(name="work_shift_reg")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkShiftReg {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Barber barber;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // ACTIVE, CANCELLED

    // audit
    @Column(nullable = false) private String createdByRole; // OWNER/BARBER
    @Column(nullable = false) private Long createdByUserId;
    @Column(nullable = false) private LocalDateTime createdAt;

    // cancel info
    private String cancelByRole;   // OWNER/BARBER
    private Long cancelByUserId;
    private String cancelReason;   // nếu BARBER hủy có lý do
    private LocalDateTime cancelAt;

    public enum Status { ACTIVE, CANCELLED }
}

