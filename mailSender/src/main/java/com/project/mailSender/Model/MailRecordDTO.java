package com.project.mailSender.Model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MailRecordDTO {

    private Long id;

    private String subject;
    private String content;
    private String targetGroup;

    private String recipientIds;
    private String recipientNames;

    private Integer recipientCount = 0;
    private Boolean hasAttachments = false;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime sendAt;

    private LocalDateTime scheduledAt;

    private Long durationMs;
}