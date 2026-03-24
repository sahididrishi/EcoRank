package dev.ecorank.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dev.ecorank.backend.entity.AdminUser;
import dev.ecorank.backend.repository.AdminUserRepository;
import dev.ecorank.backend.security.ApiKeyAuthenticationFilter;
import dev.ecorank.backend.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain for plugin-internal API routes.
     * Authenticated via X-API-Key header.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain pluginFilterChain(HttpSecurity http,
                                                  ApiKeyAuthenticationFilter apiKeyFilter) throws Exception {
        http
                .securityMatcher("/api/v1/plugin/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("PLUGIN")
                );
        return http.build();
    }

    /**
     * Security filter chain for admin API routes.
     * Authenticated via JWT Bearer token (except login/refresh/logout which are public).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http,
                                                 JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .securityMatcher("/api/v1/admin/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/admin/auth/login",
                                "/api/v1/admin/auth/refresh",
                                "/api/v1/admin/auth/logout"
                        ).permitAll()
                        .anyRequest().hasRole("ADMIN")
                );
        return http.build();
    }

    /**
     * Security filter chain for public routes: store, webhooks, actuator, swagger, SPA.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/store/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/**").permitAll()
                );
        // Allow H2 console iframe in dev
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * Create a default admin user (admin/admin) if none exists.
     */
    @Bean
    public CommandLineRunner createDefaultAdmin(AdminUserRepository adminUserRepository,
                                                 PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminUserRepository.count() == 0) {
                AdminUser admin = new AdminUser(
                        "admin",
                        passwordEncoder.encode("admin"),
                        "ADMIN"
                );
                adminUserRepository.save(admin);
                log.info("Default admin user created (username: admin, password: admin). Change this in production!");
            }
        };
    }
}
