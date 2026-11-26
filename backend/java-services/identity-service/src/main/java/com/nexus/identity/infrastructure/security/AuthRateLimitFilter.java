package com.nexus.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.error.ErrorCode;
import com.nexus.common.error.ProblemDetails;
import com.nexus.identity.config.IdentityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Set;

/** Limits unauthenticated credential endpoints per client IP and path. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/tenants");

    private final FixedWindowRateLimiter limiter;
    private final ObjectMapper mapper;

    public AuthRateLimitFilter(IdentityProperties props, ObjectMapper mapper, Clock clock) {
        this.limiter = new FixedWindowRateLimiter(props.rateLimit().requestsPerMinute(), 60_000, clock);
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long retryAfter = limiter.tryAcquire(req.getRemoteAddr() + "|" + req.getRequestURI());
        if (retryAfter > 0) {
            res.setStatus(429);
            res.setHeader("Retry-After", Long.toString(retryAfter));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(res.getOutputStream(), ProblemDetails.of(ErrorCode.RATE_LIMITED,
                    "Too many attempts. Retry in " + retryAfter + " seconds.", req.getRequestURI()));
            return;
        }
        chain.doFilter(req, res);
    }
}
