package com.project.mailSender.Model;

public record CountryEmailResponse(
        String message,
        String country,
        int totalEligible,
        int successful,
        int failed
) {}