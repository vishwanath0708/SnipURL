package com.url_shortner.SnipURL.config;

import com.url_shortner.SnipURL.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    private static final int CREATE_URL_LIMIT = 10;
    private static final int REDIRECT_LIMIT = 100;
    private static final int EXPAND_LIMIT = 30;
    private static final int DEFAULT_LIMIT = 50;
    private static final int WINDOW_SECONDS = 60;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Skip health and static resources
        if (uri.equals("/health") || uri.startsWith("/css") ||
                uri.startsWith("/js") || uri.equals("/favicon.ico")) {
            return true;
        }

        String clientIp = getClientIp(request);
        int limit = getRateLimitForEndpoint(uri, method);
        String rateKey = "rate:" + getEndpointType(uri, method) + ":" + clientIp;

        boolean allowed = rateLimitService.isAllowed(rateKey, limit, WINDOW_SECONDS);

        long remaining = rateLimitService.getRemaining(rateKey, limit);
        long resetTime = rateLimitService.getResetTime(rateKey);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));

        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(resetTime));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Limit: %d requests per %d seconds. Try again in %d seconds.\",\"limit\":%d,\"remaining\":%d,\"resetInSeconds\":%d}",
                    limit, WINDOW_SECONDS, resetTime, limit, remaining, resetTime
            ));
            return false;
        }

        return true;
    }

    private int getRateLimitForEndpoint(String uri, String method) {
        if (uri.equals("/api/shorten") && method.equals("POST")) {
            return CREATE_URL_LIMIT;
        }
        if (uri.startsWith("/api/expand")) {
            return EXPAND_LIMIT;
        }
        if (uri.matches("/[a-zA-Z0-9]+") && method.equals("GET")) {
            return REDIRECT_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    private String getEndpointType(String uri, String method) {
        if (uri.equals("/api/shorten")) return "create";
        if (uri.startsWith("/api/expand")) return "expand";
        if (uri.matches("/[a-zA-Z0-9]+")) return "redirect";
        return "other";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            return cfConnectingIp;
        }
        return request.getRemoteAddr();
    }
}