package com.project.mailSender.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @NonNull
    String name;
    @Column(unique = true, nullable = false)
    String email;
    String gender;
    boolean isSubscribed;
    String dob;
    String country;
    int age;

    private boolean deleted = false;
    private LocalDateTime deletedAt;
    LocalDate createdAt;
}
