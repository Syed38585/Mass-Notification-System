package com.project.mailSender.Repository;

import com.project.mailSender.Model.AdminModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AdminRepository extends JpaRepository<AdminModel,Long> {
    Optional<AdminModel> findByEmail(String email);

    boolean existsByEmail(String email);
}
