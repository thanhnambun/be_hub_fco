package com.ra.base_spring_boot.security.jwt;

import com.ra.base_spring_boot.exception.TokenBlacklistedException;
import com.ra.base_spring_boot.services.RedisService;
import com.ra.base_spring_boot.security.principle.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtService               jwtService;
    private final RedisService             redisService;
    private final MyUserDetailsService     userDetailsService;
    private final HandlerExceptionResolver exceptionResolver;

    @Autowired
    public JwtTokenFilter(
            JwtService jwtService,
            RedisService redisService,
            MyUserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.jwtService        = jwtService;
        this.redisService      = redisService;
        this.userDetailsService = userDetailsService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Thử đọc token từ httpOnly Cookie (ưu tiên)
            // 2. Fallback sang Authorization: Bearer header (Postman / Swagger)
            String token = extractTokenFromCookie(request);
            if (token == null) {
                token = extractBearerToken(request);
            }

            if (token != null) {
                if (redisService.exists("BL:" + token)) {
                    throw new TokenBlacklistedException("Access token has been logged out");
                }

                String username = jwtService.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            exceptionResolver.resolveException(request, response, null, ex);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Đọc access_token từ httpOnly Cookie. */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** Fallback: đọc token từ Authorization: Bearer <token> header. */
    public String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
