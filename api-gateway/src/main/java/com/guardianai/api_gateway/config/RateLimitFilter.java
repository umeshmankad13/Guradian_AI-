package com.guardianai.api_gateway.config;

import com.guardianai.api_gateway.service.AuditAction;
import com.guardianai.api_gateway.service.AuditService;
import com.guardianai.api_gateway.service.RateLimitResult;
import com.guardianai.api_gateway.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimitService rateLimitService;
    private final RateLimitProperties props;
    private final AuditService auditService;

    public RateLimitFilter(RateLimitService rateLimitService,
                           RateLimitProperties props,
                           AuditService auditService) {
        this.rateLimitService = rateLimitService;
        this.props = props;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!props.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = resolveRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimitService.check(rule.key(), rule.limit(), WINDOW);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            if (result.firstBlock()) {
                // once per window only, so blocked traffic cannot flood the audit table
                auditService.record(AuditAction.RATE_LIMITED, currentUserId(), rule.name(), request.getRemoteAddr());
            }
            long retryAfter = Math.max(result.retryAfterSeconds(), 1);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Rate limit exceeded. Try again in " + retryAfter + " seconds.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private Rule resolveRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && "/v1/auth/login".equals(path)) {
            return new Rule("login", "login:ip:" + request.getRemoteAddr(), props.loginPerMinute());
        }
        if ("POST".equals(method) && "/v1/auth/register".equals(path)) {
            return new Rule("register", "register:ip:" + request.getRemoteAddr(), props.registerPerMinute());
        }

        UUID userId = currentUserId();
        if (userId != null) {
            if ("POST".equals(method) && "/v1/scans".equals(path)) {
                return new Rule("scan", "scan:user:" + userId, props.scanPerMinute());
            }
            return new Rule("api", "api:user:" + userId, props.defaultPerMinute());
        }
        return null;
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UUID id ? id : null;
    }

    private record Rule(String name, String key, int limit) {}
}