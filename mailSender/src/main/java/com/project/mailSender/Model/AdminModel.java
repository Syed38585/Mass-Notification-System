package com.project.mailSender.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @Email
    @Column(nullable = false,unique = true)
    String email;

    @Column(nullable = false)
    String password;

    @Column(nullable = false)
    private boolean enabled = true;
}
