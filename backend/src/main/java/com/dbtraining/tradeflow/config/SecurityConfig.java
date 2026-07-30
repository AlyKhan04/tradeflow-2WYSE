@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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