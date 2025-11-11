//package com.he187383.barber.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalTime;
//
//@Entity
//@Table(name = "work_shift")
//@Getter @Setter
//@NoArgsConstructor @AllArgsConstructor
//@Builder
//@EqualsAndHashCode(of = "id")
//@ToString(exclude = "barber")
//public class WorkShift {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(optional = false, fetch = FetchType.LAZY)
//    @JoinColumn(name = "barber_id")
//    private Barber barber;
//
//    @Column(nullable = false)
//    private Integer weekday;        // 0=Mon … 6=Sun
//
//    @Column(nullable = false)
//    private LocalTime startTime;    // HH:mm
//
//    @Column(nullable = false)
//    private LocalTime endTime;      // HH:mm
//}