package com.realestate.shared.infrastructure.configuration;

import com.realestate.auth.infrastructure.security.*;
import com.realestate.shared.application.RateLimiter;
import com.realestate.shared.infrastructure.security.RequestGuard;
import com.realestate.shared.infrastructure.web.ProblemWriter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    @Order(1)
    SecurityFilterChain management(
            HttpSecurity http,
            @Value("${management.server.port}") int port,
            Environment environment)
            throws Exception {
        return http.securityMatcher(
                        request ->
                                request.getLocalPort()
                                        == environment.getProperty(
                                                "local.management.port", Integer.class, port))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/actuator/prometheus",
                                                "/actuator/info")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    SecurityFilterChain application(
            HttpSecurity http,
            JwtTokens tokens,
            AccessAuthenticator converter,
            ProblemWriter problems,
            RateLimiter limits,
            CorsConfigurationSource cors)
            throws Exception {
        // Only Authorization bearer headers authenticate requests. No authentication cookies or
        // form login.
        http.csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(cors))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/",
                                                "/health",
                                                "/api/property/all",
                                                "/api/property/single/*")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/login",
                                                "/api/auth/register",
                                                "/api/auth/refresh")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/auth/logout",
                                                "/api/auth/logout-all",
                                                "/api/auth/change-password")
                                        .authenticated()
                                        .requestMatchers("/api/property/landlord")
                                        .hasRole("LANDLORD")
                                        .requestMatchers("/api/property/tenant")
                                        .hasRole("TENANT")
                                        .requestMatchers(
                                                "/api/user/**",
                                                "/api/address/**",
                                                "/api/company/**",
                                                "/api/property/**",
                                                "/api/contract/**",
                                                "/api/transaction/**",
                                                "/api/rent/**",
                                                "/api/expense/**",
                                                "/api/management-fee/**",
                                                "/api/landlord-payment/**",
                                                "/api/report/**",
                                                "/api/upload/**",
                                                "/api/dashboard/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(
                                                jwt ->
                                                        jwt.decoder(tokens.accessDecoder())
                                                                .jwtAuthenticationConverter(
                                                                        converter))
                                        .authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        problems.write(
                                                                req,
                                                                res,
                                                                401,
                                                                "UNAUTHORIZED",
                                                                "Authentication required")))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        problems.write(
                                                                req,
                                                                res,
                                                                401,
                                                                "UNAUTHORIZED",
                                                                "Authentication required"))
                                        .accessDeniedHandler(
                                                (req, res, ex) ->
                                                        problems.write(
                                                                req,
                                                                res,
                                                                403,
                                                                "FORBIDDEN",
                                                                "Access denied")))
                .headers(
                        h ->
                                h.contentTypeOptions(c -> {})
                                        .frameOptions(f -> f.deny())
                                        .referrerPolicy(
                                                r ->
                                                        r.policy(
                                                                org.springframework.security.web
                                                                        .header.writers
                                                                        .ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .NO_REFERRER)))
                .addFilterBefore(
                        new RequestGuard(limits, problems), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource cors(AppProperties properties) {
        var values = properties.corsOrigins();
        if (values.contains("*")) throw new IllegalStateException("Use explicit CORS origins");
        var config = new CorsConfiguration();
        config.setAllowedOrigins(values);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-Device-ID",
                        "X-Request-ID",
                        "Idempotency-Key"));
        config.setExposedHeaders(
                List.of("X-Request-ID", "Retry-After", "X-Total-Count", "X-Page", "X-Page-Size"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
