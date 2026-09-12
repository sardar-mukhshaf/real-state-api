package com.realestate.shared.infrastructure.web;

import com.realestate.shared.domain.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ProblemAdvice {
    private final io.micrometer.core.instrument.MeterRegistry metrics;

    public ProblemAdvice(io.micrometer.core.instrument.MeterRegistry metrics) {
        this.metrics = metrics;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProblemAdvice.class);

    public static ProblemDetail problem(
            int status, String code, String message, HttpServletRequest request) {
        var result = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message);
        result.setTitle(HttpStatus.valueOf(status).getReasonPhrase());
        result.setType(URI.create("urn:real-estate:error:" + code.toLowerCase(Locale.ROOT)));
        result.setInstance(URI.create(request.getRequestURI()));
        result.setProperty("code", code);
        result.setProperty("timestamp", Instant.now());
        result.setProperty("traceId", Objects.toString(request.getAttribute("requestId"), ""));
        return result;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> business(
            BusinessException exception, HttpServletRequest request) {
        int status =
                switch (exception.kind()) {
                    case VALIDATION -> 400;
                    case NOT_FOUND -> 404;
                    case CONFLICT -> 409;
                    case UNAUTHORIZED -> 401;
                    case FORBIDDEN -> 403;
                    case RATE_LIMIT -> 429;
                    case UNAVAILABLE -> 503;
                };
        if (status == 401 || status == 403) {
            String action =
                    request.getRequestURI().endsWith("/refresh")
                            ? "refresh"
                            : request.getRequestURI().endsWith("/login") ? "login" : "api";
            metrics.counter(
                            "security.operation.failure",
                            "action",
                            action,
                            "code",
                            exception.kind().name())
                    .increment();
        }
        var builder = ResponseEntity.status(status);
        if (status == 401) builder.header("WWW-Authenticate", "Bearer");
        if (exception instanceof RateLimitException rate)
            builder.header("Retry-After", Long.toString(rate.retryAfter()));
        return builder.body(
                problem(status, exception.kind().name(), exception.getMessage(), request));
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ProblemDetail> validation(BindException exception, HttpServletRequest request) {
        var problem = problem(400, "VALIDATION_ERROR", "Validation failed", request);
        var fields = new TreeMap<String, String>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(
                        e ->
                                fields.putIfAbsent(
                                        e.getField(),
                                        Objects.toString(e.getDefaultMessage(), "Invalid value")));
        problem.setProperty("errors", fields);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        jakarta.validation.ConstraintViolationException.class
    })
    ResponseEntity<ProblemDetail> malformed(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(problem(400, "VALIDATION_ERROR", "Invalid request", request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(409)
                .body(problem(409, "CONFLICT", "Operation conflicts with existing data", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(403).body(problem(403, "FORBIDDEN", "Access denied", request));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> tooLarge(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(413)
                .body(
                        problem(
                                413,
                                "REQUEST_TOO_LARGE",
                                "Request exceeds configured limit",
                                request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> missing(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(404)
                .body(problem(404, "NOT_FOUND", "Route not found", request));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> method(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(405)
                .body(problem(405, "METHOD_NOT_ALLOWED", "Method not allowed", request));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> media(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(415)
                .body(problem(415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, HttpServletRequest request) {
        LOG.error("Unhandled request failure; class={}", exception.getClass().getName());
        return ResponseEntity.internalServerError()
                .body(problem(500, "INTERNAL_ERROR", "Internal server error", request));
    }
}
