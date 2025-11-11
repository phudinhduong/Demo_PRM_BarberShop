package com.he187383.barber.service;

import com.he187383.barber.dto.AuthDtos;
import com.he187383.barber.entity.*;
import com.he187383.barber.repo.*;
import com.he187383.barber.util.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final EmailVerificationRepository evRepo;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final JwtService jwt;

    private final int otpLen;
    private final int ttlMin;
    private final int maxAttempts;
    private final int cooldownSec;
    private final int maxResendPerHour;

    public AuthService(UserRepository userRepo,
                       EmailVerificationRepository evRepo,
                       PasswordEncoder encoder,
                       MailService mail,
                       JwtService jwt,
                       @Value("${app.otp.length}") int otpLen,
                       @Value("${app.otp.ttlMinutes}") int ttlMin,
                       @Value("${app.otp.maxAttempts}") int maxAttempts,
                       @Value("${app.otp.cooldownSec}") int cooldownSec,
                       @Value("${app.otp.maxResendPerHour}") int maxResendPerHour) {
        this.userRepo = userRepo;
        this.evRepo = evRepo;
        this.encoder = encoder;
        this.mail = mail;
        this.jwt = jwt;
        this.otpLen = otpLen;
        this.ttlMin = ttlMin;
        this.maxAttempts = maxAttempts;
        this.cooldownSec = cooldownSec;
        this.maxResendPerHour = maxResendPerHour;
    }

    private String genOtp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLen; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    @Transactional
    public AuthDtos.BaseRes register(AuthDtos.RegisterReq req) {
        if (userRepo.existsByEmail(req.email())) throw new IllegalArgumentException("EMAIL_TAKEN");
        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email());
        u.setPhone(req.phone());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(User.Role.CUSTOMER);
        userRepo.save(u);

        sendOtpInternal(u, true);
        return new AuthDtos.BaseRes("Verification code sent to email");
    }

    @Transactional
    public AuthDtos.BaseRes resend(AuthDtos.ResendReq req) {
        User u = userRepo.findByEmail(req.email()).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (u.getEmailVerifiedAt() != null) return new AuthDtos.BaseRes("Already verified");
        sendOtpInternal(u, false);
        return new AuthDtos.BaseRes("Resent");
    }

    private void sendOtpInternal(User u, boolean first) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        EmailVerification latest = evRepo.findLatest(u).orElse(null);
        if (latest != null) {
            if (latest.getLockedUntil() != null && latest.getLockedUntil().isAfter(now))
                throw new IllegalStateException("LOCKED");
            long seconds = Duration.between(latest.getCreatedAt(), now).getSeconds();
            if (!first && seconds < cooldownSec) throw new IllegalStateException("RATE_LIMIT");
            if (!first && latest.getResendCount() != null && latest.getResendCount() >= maxResendPerHour)
                throw new IllegalStateException("RESEND_LIMIT");
        }

        String otp = genOtp();
        EmailVerification ev = new EmailVerification();
        ev.setUser(u);
        ev.setCodeHash(encoder.encode(otp));
        ev.setPurpose("EMAIL_VERIFY");
        ev.setExpiresAt(now.plusMinutes(ttlMin));
        ev.setAttemptCount(0);
        ev.setResendCount(latest == null ? 0 : latest.getResendCount() + 1);
        ev.setLockedUntil(null);
        evRepo.save(ev);

        String body = "Hi " + (u.getName() == null ? "there" : u.getName()) + ",\n"
                + "Your verification code is: " + otp + "\n"
                + "This code expires in " + ttlMin + " minutes.\n";
        mail.send(u.getEmail(), "Your verification code", body);
    }

    @Transactional
    public AuthDtos.BaseRes verify(AuthDtos.VerifyEmailReq req) {
        User u = userRepo.findByEmail(req.email()).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        EmailVerification ev = evRepo.findLatest(u).orElseThrow(() -> new IllegalArgumentException("CODE_EXPIRED"));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (ev.getLockedUntil() != null && ev.getLockedUntil().isAfter(now))
            throw new IllegalStateException("LOCKED");
        if (ev.getExpiresAt().isBefore(now))
            throw new IllegalArgumentException("CODE_EXPIRED");

        if (!encoder.matches(req.code(), ev.getCodeHash())) {
            ev.setAttemptCount(ev.getAttemptCount() + 1);
            if (ev.getAttemptCount() >= maxAttempts) ev.setLockedUntil(now.plusMinutes(30));
            evRepo.save(ev);
            throw new IllegalArgumentException("CODE_INVALID");
        }

        u.setEmailVerifiedAt(now);
        userRepo.save(u);
        evRepo.deleteByUser(u);
        return new AuthDtos.BaseRes("Email verified");
    }

    @Transactional
    public AuthDtos.AuthRes login(AuthDtos.LoginReq req) {
        User u = userRepo.findByEmail(req.email()).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (u.getEmailVerifiedAt() == null) throw new IllegalStateException("EMAIL_UNVERIFIED");
        if (!encoder.matches(req.password(), u.getPasswordHash())) throw new IllegalArgumentException("BAD_CREDENTIALS");
        String token = jwt.generate(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthDtos.AuthRes(token, new AuthDtos.UserRes(u.getId(), u.getName(), u.getEmail(), u.getRole().name()));
    }
}
