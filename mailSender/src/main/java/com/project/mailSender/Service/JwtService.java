package com.project.mailSender.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret must not be empty"
            );
        }

        byte[] keyBytes = Decoders.BASE64.decode(secret.trim());

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must decode to at least 32 bytes for HS256"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    // =====================================================
    // ADMIN ACCESS TOKEN
    // =====================================================

    public String generateToken(String email) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(email)
                .claim("token_type", "access")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Extract Admin email only from an access token
    public String extractEmail(String token) {

        Claims claims = extractClaims(token);

        if (!"access".equals(
                claims.get("token_type", String.class))) {

            throw new JwtException(
                    "Token is not an Admin access token"
            );
        }

        return claims.getSubject();
    }

    // Validate an Admin access token
    public boolean isTokenValid(
            String token,
            String email) {

        Claims claims = extractClaims(token);

        return "access".equals(
                claims.get("token_type", String.class))
                && email.equals(claims.getSubject())
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date());
    }

    // =====================================================
    // UNSUBSCRIBE TOKEN
    // =====================================================

    public String generateUnsubscribeToken(Long userId) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("token_type", "unsubscribe")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // Extract user ID only from an unsubscribe token
    public Long extractUnsubscribeUserId(String token) {

        Claims claims = extractClaims(token);

        if (!"unsubscribe".equals(
                claims.get("token_type", String.class))) {

            throw new JwtException(
                    "Token is not an unsubscribe token"
            );
        }

        if (claims.getExpiration() == null
                || !claims.getExpiration().after(new Date())) {

            throw new JwtException(
                    "Unsubscribe token has expired"
            );
        }

        try {
            return Long.valueOf(claims.getSubject());

        } catch (NumberFormatException ex) {

            throw new JwtException(
                    "Invalid unsubscribe user ID",
                    ex
            );
        }
    }

    // =====================================================
    // COMMON JWT VALIDATION
    // =====================================================

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
