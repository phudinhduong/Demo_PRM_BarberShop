package com.he187383.barber.controller;

import com.he187383.barber.dto.BarberDtos;
import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import com.he187383.barber.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BarberController {

    private final BarberRepository repo;
    private final UserRepository userRepo;
    private final PasswordEncoder enc;
    private final JwtService jwt;

    // ===== PUBLIC =====
    @GetMapping("/barbers")
    public List<BarberDtos.BarberDto> listActive() {
        return repo.findByIsActiveTrue().stream().map(BarberDtos.BarberDto::from).toList();
    }

    // ===== OWNER GUARD =====
    private void requireOwner(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new IllegalStateException("UNAUTHORIZED");
        String role = jwt.decodeRole(h.substring(7));
        if (!"OWNER".equals(role)) throw new IllegalStateException("FORBIDDEN");
    }

    // ===== OWNER: list all =====
    @GetMapping("/owner/barbers")
    public List<BarberDtos.BarberDto> list(HttpServletRequest http) {
        requireOwner(http);
        return repo.findAll().stream().map(BarberDtos.BarberDto::from).toList();
    }

    // ===== OWNER: create (gắn user bắt buộc) =====
    @PostMapping("/owner/barbers")
    public ResponseEntity<Barber> create(@RequestBody BarberDtos.BarberCreateReq req, HttpServletRequest http) {
        requireOwner(http);

        // 1) Resolve user
        User user;
        if (req.userId() != null) {
            user = userRepo.findById(req.userId()).orElseThrow();
            // chặn trùng liên kết
            repo.findByUserId(user.getId()).ifPresent(b -> { throw new IllegalArgumentException("User đã gắn với Barber #" + b.getId()); });
            // đảm bảo role = BARBER
            if (user.getRole() != User.Role.BARBER) {
                user.setRole(User.Role.BARBER);
                userRepo.save(user);
            }
        } else {
            if (req.userEmail() == null || req.userPassword() == null) {
                throw new IllegalArgumentException("Thiếu userId hoặc (userEmail + userPassword)");
            }
            if (userRepo.findByEmail(req.userEmail()).isPresent()) {
                throw new IllegalArgumentException("Email đã tồn tại: " + req.userEmail());
            }
            user = new User();
            user.setName(req.userName() != null ? req.userName() : (req.name() != null ? req.name() : "Barber"));
            user.setEmail(req.userEmail());
            user.setPasswordHash(enc.encode(req.userPassword()));
            user.setRole(User.Role.BARBER);
            user.setEmailVerifiedAt(LocalDateTime.now());
            user = userRepo.save(user);
        }

        // 2) Tạo Barber
        Barber b = Barber.builder()
                .user(user)
                .name(req.name() != null ? req.name() : user.getName())
                .bio(req.bio())
                .avatarUrl(req.avatarUrl())
                .isActive(req.isActive() != null ? req.isActive() : true)
                .build();

        return ResponseEntity.ok(repo.save(b));
    }

    // ===== OWNER: update (không đổi user ở đây) =====
    @PutMapping("/owner/barbers/{id}")
    public ResponseEntity<Barber> update(@PathVariable Long id,
                                         @RequestBody BarberDtos.BarberUpdateReq req,
                                         HttpServletRequest http) {
        requireOwner(http);
        Barber b = repo.findById(id).orElseThrow();
        if (req.name() != null && !req.name().isBlank()) b.setName(req.name());
        if (req.bio() != null) b.setBio(req.bio());
        if (req.avatarUrl() != null) b.setAvatarUrl(req.avatarUrl());
        if (req.isActive() != null) b.setIsActive(req.isActive());
        return ResponseEntity.ok(repo.save(b));
    }

    // ===== OWNER: delete =====
    @DeleteMapping("/owner/barbers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest http) {
        requireOwner(http);
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

