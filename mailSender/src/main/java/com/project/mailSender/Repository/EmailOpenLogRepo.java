package com.project.mailSender.Repository;

import com.project.mailSender.Model.EmailOpenLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailOpenLogRepo extends JpaRepository<EmailOpenLog, Long> {

    List<EmailOpenLog> findAllByOrderByOpenedAtDesc();

    boolean existsByEmailId(String emailId);
}