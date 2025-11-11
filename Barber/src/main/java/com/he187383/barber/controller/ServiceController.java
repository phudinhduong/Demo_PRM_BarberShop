package com.he187383.barber.controller;


import com.he187383.barber.dto.ServiceDtos;
import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import com.he187383.barber.util.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceItemRepository repo;
    private final JwtService jwt;

    // ----- PUBLIC: xem danh sách dịch vụ đang bán
    @GetMapping("/services")
    public List<ServiceItem> listActive() {
        return repo.findByIsActiveTrue();
    }



    // ===== OWNER ZONE =====
    private void requireOwner(HttpServletRequest req) {

//        System.out.println(req.getHeader("Authorization"));

        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer "))
            throw new IllegalStateException("UNAUTHORIZED");
        String role = jwt.decodeRole(h.substring(7));
        if (!"OWNER".equals(role))
            throw new IllegalStateException("FORBIDDEN");
    }

    // OWNER: xem tất cả (kể cả inactive)
    @GetMapping("/owner/services")
    public ResponseEntity<List<ServiceItem>> listAll(HttpServletRequest http) {
        requireOwner(http);
        return ResponseEntity.ok(repo.findAll());
    }

    // OWNER: tạo dịch vụ
    @PostMapping("/owner/services")
    public ResponseEntity<ServiceItem> create(@RequestBody ServiceDtos.ServiceCreateReq req, HttpServletRequest http) {
        requireOwner(http);
        ServiceItem s = ServiceItem.builder()
                .name(req.name())
                .durationMin(req.durationMin())
                .price(req.price())
                .isActive(req.isActive() != null ? req.isActive() : true)
                .build();
        return ResponseEntity.ok(repo.save(s));
    }

    // OWNER: cập nhật dịch vụ
    @PutMapping("/owner/services/{id}")
    public ResponseEntity<ServiceItem> update(@PathVariable Long id,
                                              @RequestBody ServiceDtos.ServiceUpdateReq req,
                                              HttpServletRequest http) {
        requireOwner(http);
        ServiceItem s = repo.findById(id).orElseThrow();
        if (req.name() != null && !req.name().isBlank()) s.setName(req.name());
        if (req.durationMin() != null && req.durationMin() > 0) s.setDurationMin(req.durationMin());
        if (req.price() != null && req.price() >= 0) s.setPrice(req.price());
        if (req.isActive() != null) s.setIsActive(req.isActive());
        return ResponseEntity.ok(repo.save(s));
    }

    @DeleteMapping("/owner/services/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest http) {
        requireOwner(http);
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

