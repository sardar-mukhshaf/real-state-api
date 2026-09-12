package com.realestate.shared.infrastructure.web;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProblemWriter {
    private final ObjectMapper json;
    private final MeterRegistry metrics;

    public ProblemWriter(ObjectMapper json, MeterRegistry metrics) {
        this.json = json;
        this.metrics = metrics;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message)
            throws IOException {
        metrics.counter("security.requests.rejected", "code", code).increment();
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        json.writeValue(
                response.getOutputStream(), ProblemAdvice.problem(status, code, message, request));
    }
}
