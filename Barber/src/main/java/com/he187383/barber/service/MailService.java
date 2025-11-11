package com.he187383.barber.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final @Nullable JavaMailSender mailSender;

    @Value("${app.mail.mode:CONSOLE}")
    private String mode;

    @Value("${app.mail.from:no-reply@example.com}")
    private String from;

    public MailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender; // Spring tự tạo nếu spring.mail.* đầy đủ
    }

    public void send(String to, String subject, String body) {
        if ("SMTP".equalsIgnoreCase(mode) && mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
            } catch (Exception e) {
                // fallback: in console để không chặn luồng đăng ký
                System.err.println("[MAIL ERROR] " + e.getMessage());
                printToConsole(to, subject, body);
            }
        } else {
            printToConsole(to, subject, body);
        }
    }

    private void printToConsole(String to, String subject, String body) {
        System.out.println("\n=== MAIL DEBUG ===");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println(body);
        System.out.println("==================\n");
    }
}