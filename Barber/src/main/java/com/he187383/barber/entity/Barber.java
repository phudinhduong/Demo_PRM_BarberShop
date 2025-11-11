package com.he187383.barber.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "barbers",
        uniqueConstraints = @UniqueConstraint(name = "uk_barber_user", columnNames = "user_id")
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi Barber gắn với đúng 1 User (role = BARBER)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_barber_user"))
    private User user;

    @Column(nullable = false)
    private String name;       // Tên thợ

    private String bio;        // Mô tả ngắn
    private String avatarUrl;  // Ảnh đại diện

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
