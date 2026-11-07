package com.project.mailSender.Model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TemplateRequest {


        @NotBlank(message = "Template name is required")
        private String name;

        @NotBlank(message = "Subject is required")
        private String subject;

        @NotBlank(message = "HTML body is required")
        private String htmlBody;

        private Boolean favorite;
}
