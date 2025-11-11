package com.he187383.barber.dto;

import com.he187383.barber.entity.Barber;

public class BarberDtos {

    public record BarberCreateReq(
            Long userId,
            String userEmail,
            String userPassword,
            String userName,

            String name,
            String bio,
            String avatarUrl,
            Boolean isActive
    ) {}

    public record BarberUpdateReq(
            String name,
            String bio,
            String avatarUrl,
            Boolean isActive
    ) {}

    // BarberDto.java
    public record BarberDto(Long id, String name, String bio, String avatarUrl, Boolean isActive, Long userId, String userEmail) {
        public static BarberDto from(Barber b) {
            var u = b.getUser();
            return new BarberDto(
                    b.getId(), b.getName(), b.getBio(), b.getAvatarUrl(), b.getIsActive(),
                    u != null ? u.getId() : null,
                    u != null ? u.getEmail() : null
            );
        }
    }

}
