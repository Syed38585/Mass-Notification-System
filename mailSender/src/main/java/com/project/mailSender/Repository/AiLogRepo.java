package com.project.mailSender.Repository;

import com.project.mailSender.Model.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiLogRepo extends JpaRepository<AiLog, Long> {
    List<AiLog> findByRequestedAtAfter(LocalDateTime time);
}