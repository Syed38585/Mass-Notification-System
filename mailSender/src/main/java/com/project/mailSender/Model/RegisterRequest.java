package com.project.mailSender.Model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    String name;

    @NotBlank
    String email;

    @NotBlank
    @Size(min = 6, max = 72,message = "Password length must be between 8 and 24 characters")
    String password;
}
