package com.he187383.barber.controller;

import com.he187383.barber.dto.AuthDtos;
import com.he187383.barber.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterReq req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resend(@Valid @RequestBody AuthDtos.ResendReq req) {
        return ResponseEntity.ok(authService.resend(req));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verify(@Valid @RequestBody AuthDtos.VerifyEmailReq req) {
        return ResponseEntity.ok(authService.verify(req));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginReq req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
