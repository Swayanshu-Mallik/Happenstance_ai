
package com.happenstance.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtFilter jwtFilter(
            @Value("${happenstance.jwt.secret}") String secret) {
        return new JwtFilter(secret);
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtFilter filter) throws Exception {

        return http
                // Authentication uses Authorization: Bearer tokens,
                // not browser-managed session cookies.
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/api/v1/health",
                                "/api/v1/auth/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, ex) -> {
                                    response.setStatus(
                                            HttpStatus.UNAUTHORIZED.value()
                                    );
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.getWriter().write(
                                            "{\"error\":\"Authentication required or token invalid\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, ex) -> {
                                    response.setStatus(
                                            HttpStatus.FORBIDDEN.value()
                                    );
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.getWriter().write(
                                            "{\"error\":\"Access denied\"}"
                                    );
                                }
                        )
                )

                .addFilterBefore(
                        filter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    @Bean
    CorsConfigurationSource cors(
            @Value("${happenstance.cors.allowed-origin}") String origin) {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(origin));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    static class JwtFilter extends OncePerRequestFilter {

        private final SecretKey key;

        JwtFilter(String secret) {

            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "JWT_SECRET must be configured."
                );
            }

            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

            if (bytes.length < 32) {
                throw new IllegalStateException(
                        "JWT_SECRET must contain at least 32 bytes."
                );
            }

            this.key = Keys.hmacShaKeyFor(bytes);
        }

        @Override
        protected boolean shouldNotFilter(
                HttpServletRequest request) {

            return request.getServletPath()
                    .startsWith("/api/v1/auth/");
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain chain)
                throws ServletException, IOException {

            String authorization =
                    request.getHeader("Authorization");

            if (authorization != null
                    && authorization.startsWith("Bearer ")) {

                String token = authorization.substring(7).trim();

                try {

                    if (token.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Missing bearer token."
                        );
                    }

                    String subject = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload()
                            .getSubject();

                    if (subject == null || subject.isBlank()) {
                        throw new IllegalArgumentException(
                                "JWT subject is missing."
                        );
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    subject,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_USER"
                                            )
                                    )
                            );

                    org.springframework.security.core.context
                            .SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);

                } catch (io.jsonwebtoken.JwtException |
                         IllegalArgumentException exception) {

                    org.springframework.security.core.context
                            .SecurityContextHolder
                            .clearContext();

                    response.setStatus(
                            HttpStatus.UNAUTHORIZED.value()
                    );

                    response.setContentType("application/json");

                    response.getWriter().write(
                            "{\"error\":\"Invalid or expired token\"}"
                    );

                    return;
                }
            }

            chain.doFilter(request, response);
        }
    }
}
