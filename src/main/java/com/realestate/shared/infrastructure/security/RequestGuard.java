package com.realestate.shared.infrastructure.security;

import com.realestate.shared.application.RateLimiter;
import com.realestate.shared.domain.*;
import com.realestate.shared.infrastructure.web.ProblemWriter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestGuard extends OncePerRequestFilter {
    private final RateLimiter limits;
    private final ProblemWriter problems;

    public RequestGuard(RateLimiter limits, ProblemWriter problems) {
        this.limits = limits;
        this.problems = problems;
    }

    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || request.getMethod().equals("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            limits.check("GLOBAL", "ip:" + request.getRemoteAddr());
            String policy =
                    path.equals("/api/auth/login")
                            ? "LOGIN"
                            : path.equals("/api/auth/register")
                                    ? "REGISTER"
                                    : path.equals("/api/auth/refresh")
                                            ? "REFRESH"
                                            : path.equals("/api/auth/change-password")
                                                    ? "PASSWORD_RESET"
                                                    : path.startsWith("/api/report/")
                                                            ? "STRICT"
                                                            : (path.startsWith("/api/upload")
                                                                            || path.contains(
                                                                                    "/images/")
                                                                            || path.contains(
                                                                                    "/documents/"))
                                                                    ? "UPLOAD"
                                                                    : "API";
            limits.check(policy, "ip:" + request.getRemoteAddr());
            if (request.getContentType() != null
                    && request.getContentType()
                            .toLowerCase(Locale.ROOT)
                            .startsWith("application/json")) {
                int max = 1024 * 1024;
                if (request.getContentLengthLong() > max) {
                    problems.write(
                            request, response, 413, "REQUEST_TOO_LARGE", "JSON body exceeds 1 MB");
                    return;
                }
                byte[] bytes = request.getInputStream().readNBytes(max + 1);
                if (bytes.length > max) {
                    problems.write(
                            request, response, 413, "REQUEST_TOO_LARGE", "JSON body exceeds 1 MB");
                    return;
                }
                var wrapped =
                        new HttpServletRequestWrapper(request) {
                            public ServletInputStream getInputStream() {
                                var input = new ByteArrayInputStream(bytes);
                                return new ServletInputStream() {
                                    public int read() {
                                        return input.read();
                                    }

                                    public int read(byte[] target, int off, int len) {
                                        return input.read(target, off, len);
                                    }

                                    public boolean isFinished() {
                                        return input.available() == 0;
                                    }

                                    public boolean isReady() {
                                        return true;
                                    }

                                    public void setReadListener(ReadListener listener) {
                                        throw new IllegalStateException("Synchronous request body");
                                    }
                                };
                            }

                            public BufferedReader getReader() {
                                return new BufferedReader(
                                        new InputStreamReader(
                                                getInputStream(),
                                                java.nio.charset.StandardCharsets.UTF_8));
                            }
                        };
                chain.doFilter(wrapped, response);
            } else chain.doFilter(request, response);
        } catch (RateLimitException ex) {
            response.setHeader("Retry-After", Long.toString(ex.retryAfter()));
            problems.write(request, response, 429, "RATE_LIMIT", ex.getMessage());
        } catch (BusinessException ex) {
            if (ex.kind() == BusinessException.Kind.UNAVAILABLE)
                problems.write(request, response, 503, "UNAVAILABLE", ex.getMessage());
            else throw ex;
        }
    }
}
