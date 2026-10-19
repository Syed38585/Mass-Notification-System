package com.project.mailSender.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;

@Data
@Entity
@NoArgsConstructor
public class Users{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @NonNull
    String name;
    @Column(unique = true, nullable = false)
    String email;
    String gender;
    @Value("${boolean.isPrime:false}")
    boolean isPrime;
}
