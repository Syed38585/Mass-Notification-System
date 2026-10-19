package com.project.mailSender.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class MailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String targetGroup;

    private LocalDateTime sentAt;

    private long durationMs;

    private int totalRecipients;
}