package com.project.mailSender.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DraftRequest {

    private Long id;

    private String subject;

    private String content;

    private String targetGroup;

    private String recipientIds;
}