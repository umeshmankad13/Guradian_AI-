package com.guardianai.api_gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.guardianai.api_gateway.service.JwtService;
import com.guardianai.api_gateway.service.RateLimitService;

@Configuration
@EnableConfigurationProperties (RateLimitProperties.class)
public class SecurityConfig {

    private final JwtService jwtService;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitService rateLimitService;

    public SecurityConfig(JwtService jwtService, RateLimitService rateLimitService, RateLimitProperties rateLimitProperties) {
        this.jwtService = jwtService;
        this.rateLimitService=rateLimitService;
        this.rateLimitProperties=rateLimitProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) -> {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\"}");
            }))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/v1/health", "/v1/auth/**", "/error", "/v1/auth/**").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new RateLimitFilter(rateLimitService, rateLimitProperties), JwtAuthenticationFilter.class);
        return http.build();
    }
}