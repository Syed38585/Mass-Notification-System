package com.project.mailSender.Model;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public record CountryEmailRequest(

        @NotBlank(message = "Country is required")
        String country,

        @NotBlank(message = "Email subject is required")
        String subject,

        @NotBlank(message = "Email body is required")
        String body

) {}