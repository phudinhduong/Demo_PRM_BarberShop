package com.he187383.barber.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterReq(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Pattern(regexp = "^[0-9+\\-()\\s]{0,20}$", message = "Invalid phone") String phone,
            @Size(min = 6) String password
    ) { }

    public record VerifyEmailReq(
            @Email @NotBlank String email,
            @Pattern(regexp = "^\\d{6}$") String code
    ) { }

    public record ResendReq(@Email @NotBlank String email) { }

    public record LoginReq(@Email @NotBlank String email, @NotBlank String password) { }

    public record BaseRes(String message) { }

    public record UserRes(Long id, String name, String email, String role) { }

    public record AuthRes(String token, UserRes user) { }
}
