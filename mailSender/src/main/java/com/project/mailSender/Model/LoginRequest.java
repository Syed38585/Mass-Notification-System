package com.project.mailSender.Model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
    @Email
    @NotBlank
    String email;
    @NotBlank
    String password;
}
