package com.washer.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class QueryParameterValidationInterceptor implements HandlerInterceptor {

    static final long MAX_PAGE = 100_000L;
    static final long MAX_PAGE_SIZE = 100L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        validate(request, "page", MAX_PAGE);
        validate(request, "size", MAX_PAGE_SIZE);
        validate(request, "limit", MAX_PAGE_SIZE);
        return true;
    }

    private void validate(HttpServletRequest request, String name, long max) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) return;
        final long value;
        try {
            value = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        if (value < 1 || value > max) {
            throw new IllegalArgumentException(name + " must be between 1 and " + max);
        }
    }
}
