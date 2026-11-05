package com.project.mailSender.Controller;

import com.project.mailSender.Model.AdminModel;
import com.project.mailSender.Model.LoginRequest;
import com.project.mailSender.Model.AuthResponse;
import com.project.mailSender.Model.RegisterRequest;
import com.project.mailSender.Repository.AdminRepository;
import com.project.mailSender.Service.JwtService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationMs;

    public AuthController(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${jwt.expiration-ms}") long expirationMs) {

        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (adminRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Email is already registered"
                    ));
        }

        AdminModel admin = new AdminModel();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setEnabled(true);

        AdminModel savedAdmin = adminRepository.save(admin);

        // Generate JWT for the newly registered admin
        String token = jwtService.generateToken(savedAdmin.getEmail());

        // Return the authentication response
        AuthResponse authResponse = new AuthResponse(
                token,
                "Bearer",
                3600
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authResponse);
    }


    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            request.getPassword()
                    )
            );

            String token = jwtService.generateToken(email);

            return ResponseEntity.ok(
                    new AuthResponse(
                            token,
                            "Bearer",
                            expirationMs / 1000
                    )
            );

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }
    }
}