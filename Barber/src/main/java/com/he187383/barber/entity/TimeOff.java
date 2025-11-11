package com.he187383.barber.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "time_offs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimeOff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_timeoff_barber"))
    private Barber barber;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allDay = false; // true = nghỉ cả ngày

    // Với allDay=false thì start/end phải khác null
    private LocalTime startTime;
    private LocalTime endTime;

    private String reason;

    // audit
    @Column(nullable = false) private String createdByRole; // OWNER
    @Column(nullable = false) private Long createdByUserId;
    @Column(nullable = false) private LocalDateTime createdAt;
}
