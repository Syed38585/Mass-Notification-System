package com.project.mailSender.Repository;

import com.project.mailSender.Model.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiLogRepo extends JpaRepository<AiLog, Long> {}