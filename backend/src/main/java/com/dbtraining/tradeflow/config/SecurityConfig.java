package com.dbtraining.tradeflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ============================================================================
 * SecurityConfig — TICKET-I076 + TICKET-I077
 * ============================================================================
 * WHAT:    Spring Security HTTP rules + in-memory user store.
 * HOW:     Single SecurityFilterChain @Bean + InMemoryUserDetailsManager.
 * WHY:     Day 6 needs role-based protection on every endpoint.
 * OBSERVE: After Day-6 work is wired, GET /api/v1/trades without auth → 401.
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

    /**
     * CORS for the browser front-ends (Day 8 static dashboard, Day 9 Vite app).
     * Both are served from a different origin than the API, so without this the
     * browser blocks every fetch before it reaches Spring. Origins are listed
     * explicitly rather than using "*", and only the two headers the front-ends
     * actually send are allowed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5500", "http://127.0.0.1:5500",   // static-dashboard
                "http://localhost:5173", "http://127.0.0.1:5173"    // Vite dev server
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // h2-console renders in an iframe on the same origin — allow it in dev.
                .headers(h -> h.frameOptions(f -> f.disable()))
                .authorizeHttpRequests(auth -> auth
                        // Open endpoints (also scraped by Prometheus)
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()

                        // Actuator (beyond health/info/prometheus) — admin only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Trade + recon API — role-per-method
                        .requestMatchers(HttpMethod.GET,    "/api/v1/**").hasRole("VIEWER")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/**").hasRole("TRADER")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/**").hasRole("TRADER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("TRADER")

                        .anyRequest().authenticated())
                .httpBasic(b -> {})
                .build();
    }
}
