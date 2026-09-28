package com.rms.config;

import com.rms.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        // ALWAYS allow CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow Spring Boot's error forward (so real exceptions surface, not 403)
                        .requestMatchers("/error").permitAll()

                        // Public auth endpoints
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()

                        // Logout must work with an expired/missing access token
                        .requestMatchers("/api/auth/logout").permitAll()

                        // Password reset endpoints
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/reset-password").permitAll()

                        // Test endpoints - email test is public; other test endpoints require authentication
                        .requestMatchers("/api/test/email-test").permitAll()
                        .requestMatchers("/api/test/**").authenticated()

                        // ArifPay gateway callback - PUBLIC
                        .requestMatchers("/api/payments/arifpay/notify").permitAll()

                        // Register - ADMIN only
                        .requestMatchers("/api/auth/register").hasRole("ADMIN")

                        // User profile - authenticated
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // ADMIN only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // ADMIN + TENANT
                        .requestMatchers("/api/tenant/**").hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers("/api/spaces/**").hasRole("ADMIN")
                        .requestMatchers("/api/tenants/**").hasRole("ADMIN")
                        .requestMatchers("/api/leases/**").hasRole("ADMIN")
                        .requestMatchers("/api/invoices/**").hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers("/api/payments/**").hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")

                        // Documents
                        .requestMatchers("/api/documents/upload/**").hasRole("ADMIN")
                        .requestMatchers("/api/documents/tenant/**").hasRole("ADMIN")
                        .requestMatchers("/api/documents/delete/**").hasRole("ADMIN")
                        .requestMatchers("/api/documents/my-documents").hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers("/api/documents/download/**").hasAnyRole("ADMIN", "TENANT")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}