package com.fco.platform.auth.infrastructure.jwt;

import com.fco.platform.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final Key signingKey;
    private final Duration accessTokenTtl;
    private final long clockSkewSeconds;

    public JwtService(
            @Value("${jwt.secret.key}") String secret,
            @Value("${jwt.expired.access}") long accessTokenTtlMs,
            @Value("${jwt.clock-skew-seconds:10}") long clockSkewSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMillis(accessTokenTtlMs);
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public String generateAccessToken(UserDetails userDetails) {
        Instant now = Instant.now();
        
        Long userId = null;
        if (userDetails instanceof User user) {
            userId = user.getId();
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        if (userId != null) {
            claims.put("uid", userId);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public long getRemainingTtlMillis(String token) {
        long remaining = extractExpiration(token).getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0L);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && getRemainingTtlMillis(token) > 0;
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
