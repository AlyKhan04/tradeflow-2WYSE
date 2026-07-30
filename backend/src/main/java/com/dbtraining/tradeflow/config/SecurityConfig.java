package com.dbtraining.tradeflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ============================================================================
 * SecurityConfig — TICKET-I076 + TICKET-I077
 * ============================================================================
 * WHAT:    Spring Security HTTP rules + in-memory user store.
 * HOW:     Single SecurityFilterChain @Bean + InMemoryUserDetailsManager.
 * WHY:     Day 6 needs role-based protection on every endpoint.
 * OBSERVE: After Day-6 work is wired, GET /api/v1/trades without auth → 401.
 * ============================================================================
 *
 *  DAY-1 DEFAULT (below): everything is `permitAll`. This lets the frontend
 *  load on Day 1 without an auth UI. TICKET-I076 + I077 replace this with
 *  proper role-based auth (admin / trader / viewer).
 * ============================================================================
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public InMemoryUserDetailsManager users(PasswordEncoder encoder) {
        UserDetails viewer = User.withUsername("viewer")
                .password(encoder.encode("viewer-pw"))
                .roles("VIEWER").build();
        UserDetails trader = User.withUsername("trader")
                .password(encoder.encode("trader-pw"))
                .roles("VIEWER", "TRADER").build();
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin-pw"))
                .roles("VIEWER", "TRADER", "ADMIN").build();
        return new InMemoryUserDetailsManager(viewer, trader, admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // h2-console renders in an iframe on the same origin — allow it in dev.
                .headers(h -> h.frameOptions(f -> f.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        // Everything else in /actuator/** is admin-only (extended in I077).
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(b -> {})
                .build();
    }
}