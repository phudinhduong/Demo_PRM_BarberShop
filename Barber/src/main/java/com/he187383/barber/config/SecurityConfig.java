package com.he187383.barber.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http.csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(reg -> reg
//                        // mở cho auth & public
//                        .requestMatchers("/api/auth/**").permitAll()
//                        .requestMatchers("/api/services/**", "/api/barbers/**","/api/payment/**","/api/shifts/**","/api/barber/**").permitAll()
//                        .requestMatchers("/api/shifts/**").permitAll() // để controller tự kiểm tra role qua JWT
//                        .requestMatchers("/api/shifts/week-grid", "/api/shifts/day-grid").permitAll()
//                        .requestMatchers("/api/customer/**").permitAll()
//
//
//
//                        // mở cho owner để controller tự kiểm tra Bearer/role
//                        .requestMatchers("/api/owner/**").permitAll()
//                        // các API khác yêu cầu đăng nhập (nếu có)
//                        .anyRequest().authenticated()
//                )
//                .httpBasic(b -> b.disable())
//                .formLogin(f -> f.disable());
//        return http.build();
//    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/services/**", "/api/barbers/**","/api/payment/**","/api/shifts/**","/api/barber/**").permitAll()
                        .requestMatchers("/api/shifts/week-grid", "/api/shifts/day-grid").permitAll()
                        .requestMatchers("/api/customer/**").permitAll()

                        // owner: cân nhắc bắt buộc auth
                        .requestMatchers("/api/owner/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable());
        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        // Ngrok domain đổi subdomain → dùng allowedOriginPatterns
        c.setAllowedOrigins(List.of("http://127.0.0.1:5500","http://localhost:5500"));
        c.setAllowedOriginPatterns(List.of(
                "https://*.ngrok-free.app",
                "https://*.ngrok.app",
                "https://app.tenmiencuabe.com" // nếu có web front-end
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With","Origin"));
        c.setExposedHeaders(List.of("Authorization","Location"));
        c.setAllowCredentials(true);
        c.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Component
    public class JwtAuthFilter extends OncePerRequestFilter {
        private static final AntPathMatcher PM = new AntPathMatcher();
        private static final String[] SKIP = {
                "/actuator/**", "/api/auth/**", "/error", "/favicon.ico"
        };

        @Override
        protected boolean shouldNotFilter(HttpServletRequest req) {
            String p = req.getRequestURI();
            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
            for (String s : SKIP) if (PM.match(s, p)) return true;
            return false;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain fc)
                throws ServletException, IOException {
            String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                // parse & set SecurityContext...
            }
            // Quan trọng: KHÔNG ném exception nếu header thiếu/parse fail → trả 401 ở layer Security
            fc.doFilter(req, res);
        }
    }
}
