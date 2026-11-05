package com.project.mailSender;


import com.project.mailSender.Service.AdminDetailsService;
import com.project.mailSender.Service.JwtService;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminDetailsService adminDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AdminDetailsService adminDetailsService) {

        this.jwtService = jwtService;
        this.adminDetailsService = adminDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String email = jwtService.extractEmail(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails user =
                        adminDetailsService.loadUserByUsername(email);

                if (!user.isEnabled()
                        || !jwtService.isTokenValid(token, user.getUsername())) {
                    unauthorized(response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException
                 | UsernameNotFoundException ex) {

            SecurityContextHolder.clearContext();
            unauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Invalid or expired token\"}"
        );
    }
}