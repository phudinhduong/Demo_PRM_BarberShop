package com.he187383.barber.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="work_shift_log")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkShiftLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WorkShiftReg shift;

    @Column(nullable = false) private String action;    // CREATED / CANCELLED
    @Column(nullable = false) private String actorRole; // OWNER/BARBER
    @Column(nullable = false) private Long actorUserId;
    private String reason;

    @Column(nullable = false) private LocalDateTime at;
}

