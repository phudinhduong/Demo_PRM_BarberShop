package com.he187383.barber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        // mở cho auth & public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/services/**", "/api/barbers/**","/api/payment/**","/api/shifts/**","/api/barber/**").permitAll()
                        .requestMatchers("/api/shifts/**").permitAll() // để controller tự kiểm tra role qua JWT
                        .requestMatchers("/api/shifts/week-grid", "/api/shifts/day-grid").permitAll()
                        .requestMatchers("/api/customer/**").permitAll()



                        // mở cho owner để controller tự kiểm tra Bearer/role
                        .requestMatchers("/api/owner/**").permitAll()
                        // các API khác yêu cầu đăng nhập (nếu có)
                        .anyRequest().authenticated()
                )
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable());
        return http.build();
    }
}
