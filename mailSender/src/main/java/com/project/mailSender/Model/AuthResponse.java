package com.project.mailSender.Model;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}
