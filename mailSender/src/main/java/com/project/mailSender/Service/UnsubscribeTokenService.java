package com.project.mailSender.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class UnsubscribeTokenService {

    private final SecretKey secretKey;

    public UnsubscribeTokenService(
            @Value("${unsubscribe.jwt.secret}") String secret
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 characters"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }


    // ==========================================
    // Generate unsubscribe JWT
    // ==========================================

    public String generateToken(Long userId) {

        Date now = new Date();

        // Token valid for 24 hours
        Date expiry = new Date(
                now.getTime() + (24 * 60 * 60 * 1000)
        );

        return Jwts.builder()

                // User ID
                .subject(userId.toString())

                // This JWT can ONLY be used
                // for unsubscribe
                .claim("purpose", "unsubscribe")

                .issuedAt(now)

                .expiration(expiry)

                .signWith(secretKey)

                .compact();
    }


    // ==========================================
    // Validate token and return user ID
    // ==========================================

    public Long validateToken(String token) {

        Claims claims = Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();


        String purpose = claims.get(
                "purpose",
                String.class
        );


        if (!"unsubscribe".equals(purpose)) {

            throw new IllegalArgumentException(
                    "Invalid unsubscribe token"
            );
        }


        return Long.parseLong(
                claims.getSubject()
        );
    }
}