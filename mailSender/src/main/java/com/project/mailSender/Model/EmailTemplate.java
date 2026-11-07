package com.project.mailSender.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_templates")
@Getter @Setter
public class EmailTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(nullable = false, length = 250)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String htmlBody;

    @Column(nullable = false)
    private boolean favorite = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() { updatedAt = LocalDateTime.now(); }
}
