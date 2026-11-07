package com.project.mailSender.Repository;

import com.project.mailSender.Model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    List<EmailTemplate> findAllByOrderByUpdatedAtDesc();
    List<EmailTemplate> findByFavoriteTrueOrderByUpdatedAtDesc();
}
