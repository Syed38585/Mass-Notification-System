package com.project.mailSender.Model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class TemplateSendRequest {
    // Empty/null recipientIds means all subscribed users.
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "HTML body is required")
    private String htmlBody;

    private String targetGroup;
    private List<Long> recipientIds;
}
