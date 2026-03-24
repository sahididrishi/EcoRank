package dev.ecorank.backend.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String bucketKey = clientIp + ":" + getPathPrefix(path);

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(path));

        if (bucket.tryConsume(1)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
        );
        return false;
    }

    private Bucket createBucket(String path) {
        Bandwidth limit;

        if (path.startsWith("/api/v1/store")) {
            limit = Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
        } else if (path.startsWith("/api/v1/webhooks")) {
            limit = Bandwidth.builder().capacity(100).refillGreedy(100, Duration.ofMinutes(1)).build();
        } else if (path.startsWith("/api/v1/plugin")) {
            limit = Bandwidth.builder().capacity(120).refillGreedy(120, Duration.ofMinutes(1)).build();
        } else if (path.startsWith("/api/v1/admin")) {
            limit = Bandwidth.builder().capacity(300).refillGreedy(300, Duration.ofMinutes(1)).build();
        } else {
            limit = Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
        }

        return Bucket.builder().addLimit(limit).build();
    }

    private String getPathPrefix(String path) {
        if (path.startsWith("/api/v1/store")) return "store";
        if (path.startsWith("/api/v1/webhooks")) return "webhooks";
        if (path.startsWith("/api/v1/plugin")) return "plugin";
        if (path.startsWith("/api/v1/admin")) return "admin";
        return "default";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
