 package com.example.LoanMatrix.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${rate-limit.max-requests:60}")
    private int maxRequests;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, RequestCounter> requestCounts =
            new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        long currentTime = System.currentTimeMillis();

        RequestCounter counter = requestCounts.compute(clientIp,
                (key, existingCounter) -> {
                    if (existingCounter == null
                            || currentTime - existingCounter.startTime
                            >= windowSeconds * 1000L) {
                        return new RequestCounter(currentTime);
                    }

                    return existingCounter;
                });

        int count = counter.requestCount.incrementAndGet();

        if (count > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestCounter {

        private final long startTime;
        private final AtomicInteger requestCount = new AtomicInteger(0);

        private RequestCounter(long startTime) {
            this.startTime = startTime;
        }
    }
}
