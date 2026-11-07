package com.project.mailSender.Service;

import com.project.mailSender.Model.AiLog;
import com.project.mailSender.Repository.AiLogRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AiLogService {

    private final AiLogRepo aiLogRepo;

    private static final ZoneId IST_ZONE =
            ZoneId.of("Asia/Kolkata");

    public AiLogService(AiLogRepo aiLogRepo) {
        this.aiLogRepo = aiLogRepo;
    }

    @Transactional
    public void saveAiRequest() {

        AiLog log = new AiLog();

        log.setRequestedAt(
                LocalDateTime.now(IST_ZONE)
        );

        aiLogRepo.save(log);
    }
}