package com.he187383.barber.controller;

import com.he187383.barber.entity.Booking;
import com.he187383.barber.repo.BookingRepository;
import com.he187383.barber.service.VnPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class VnPayCallbackController {
    private final VnPayService vnp;
    private final BookingRepository bookingRepo;

    // User browser redirect
    @GetMapping("/vnpay-return")
    public ResponseEntity<String> returnUrl(HttpServletRequest req) {
        Map<String,String> params = collect(req.getParameterMap());
        String secure = params.remove("vnp_SecureHash");
        if (!vnp.verify(params, secure)) return ResponseEntity.badRequest().body("INVALID HASH");

        String txnRef = params.get("vnp_TxnRef");
        String rsp    = params.getOrDefault("vnp_ResponseCode","99"); // "00" = success

        var booking = findByTxnRef(txnRef);
        if (booking == null) return ResponseEntity.badRequest().body("BOOKING NOT FOUND");

        if ("00".equals(rsp)) booking.setStatus(Booking.Status.PAID);
        else booking.setStatus(Booking.Status.CANCELLED);
        bookingRepo.save(booking);

        return ResponseEntity.ok("Booking #" + booking.getId() + " status: " + booking.getStatus());
    }

    // (tuỳ chọn) IPN – nếu cần xác nhận server-to-server
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> ipn(HttpServletRequest req) {
        Map<String,String> params = collect(req.getParameterMap());
        String secure = params.remove("vnp_SecureHash");
        if (!vnp.verify(params, secure)) return ResponseEntity.ok("INVALID");

        String txnRef = params.get("vnp_TxnRef");
        String rsp    = params.getOrDefault("vnp_ResponseCode","99");

        var booking = findByTxnRef(txnRef);
        if (booking == null) return ResponseEntity.ok("NOT_FOUND");

        if ("00".equals(rsp)) booking.setStatus(Booking.Status.PAID);
        else booking.setStatus(Booking.Status.CANCELLED);
        bookingRepo.save(booking);

        return ResponseEntity.ok("OK");
    }

    private Map<String,String> collect(Map<String,String[]> m){
        Map<String,String> r = new HashMap<>();
        for (var e : m.entrySet()) r.put(e.getKey(), e.getValue()[0]);
        return r;
    }
    private com.he187383.barber.entity.Booking findByTxnRef(String ref){
        return bookingRepo.findAll().stream()
                .filter(b -> ref.equals(b.getPaymentRef()))
                .findFirst().orElse(null);
    }
}

