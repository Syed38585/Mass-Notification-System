package com.project.mailSender.Model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "mail_records")
public class MailRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String targetGroup;

    @Column(columnDefinition = "TEXT")
    private String recipientIds;

    @Column(columnDefinition = "TEXT")
    private String recipientNames;

    private Integer recipientCount = 0;

    private Boolean hasAttachments = false;

    @Column(length = 30)
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;

    private LocalDateTime sendAt;

    private Long durationMs;

    private LocalDateTime scheduledAt;

    @PrePersist
    public void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}