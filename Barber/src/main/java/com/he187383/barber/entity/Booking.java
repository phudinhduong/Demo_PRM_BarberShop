package com.he187383.barber.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Booking.java
@Entity @Table(name="bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    public enum Status { PENDING, PAID, CANCELLED, COMPLETED }
    public enum PayMethod { CASH, VNPAY }   // đổi PAY_LATER -> CASH

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id")
    private User user;

    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="barber_id")
    private Barber barber;

    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="service_id")
    private ServiceItem service;

    @Column(nullable=false) private java.time.LocalDateTime startDt;
    @Column(nullable=false) private java.time.LocalDateTime endDt;

    @Column(nullable=false) private Integer price;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Status status;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private PayMethod payMethod;          // CASH hoặc VNPAY

    private String paymentRef;            // dùng khi VNPAY
    private java.time.LocalDateTime createdAt;
}
