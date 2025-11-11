package com.he187383.barber.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {
    private final Key key;
    private final int expiryMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiryMinutes}") int expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiryMinutes = expiryMinutes;
    }

    public String generate(Long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of("email", email, "role", role))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expiryMinutes * 60L)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    public String decodeRole(String token){
        Object r = getClaims(token).get("role");
        return r == null ? "" : r.toString();
    }

    public Long decodeUserId(String token){
        String sub = getClaims(token).getSubject();
        try { return Long.parseLong(sub); }
        catch (NumberFormatException e) { return null; }
    }

    // (tuỳ chọn) nếu cần lấy email
    public String decodeEmail(String token){
        Object e = getClaims(token).get("email");
        return e == null ? "" : e.toString();
    }
}
