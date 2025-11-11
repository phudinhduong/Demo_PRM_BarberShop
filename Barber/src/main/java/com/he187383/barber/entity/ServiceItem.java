package com.he187383.barber.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "services")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer durationMin;   // phút: 30,45,60...

    @Column(nullable = false)
    private Integer price;         // VND

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
