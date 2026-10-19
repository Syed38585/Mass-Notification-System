package com.project.mailSender.AiStuff;

import com.project.mailSender.Repository.AiLogRepo;
import com.project.mailSender.Repository.EmailOpenLogRepo;
import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Repository.Repo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class AnalyticsAgent {

    Repo userRepo;
    MailLogRepo mailLogRepo;
    AiLogRepo aiLogRepo;
    EmailOpenLogRepo emailOpenLogRepo;

    public AnalyticsAgent(Repo userRepo, MailLogRepo mailLogRepo, AiLogRepo aiLogRepo, EmailOpenLogRepo emailOpenLogRepo) {
        this.userRepo = userRepo;
        this.mailLogRepo = mailLogRepo;
        this.aiLogRepo = aiLogRepo;
        this.emailOpenLogRepo = emailOpenLogRepo;
    }

    @Tool(description = "Tool to get all the analytics")
    public Map<String, Object> dashboardSummary() {

        Map<String, Object> map = new HashMap<>();

        long totalUsers = userRepo.count();
        long totalMails = mailLogRepo.count();
        long totalAi = aiLogRepo.count();
        long opens = emailOpenLogRepo.count();

        map.put("totalUsers", totalUsers);
        map.put("maleUsers", userRepo.findByGender("male").size());
        map.put("femaleUsers", userRepo.findByGender("female").size());
        map.put("totalMails", totalMails);
        map.put("mailsToday",
                mailLogRepo.findBySentAtAfter(
                        LocalDateTime.now().minusDays(1)
                ).size());
        map.put("aiRequests", totalAi);
//        map.put("emailOpens", opens);

        map.put("avgSendTime",
                mailLogRepo.findAverageDuration());

        map.put("lastDuration",
                mailLogRepo.findTopByOrderBySentAtDesc()
                        .map(log -> log.getDurationMs())
                        .orElse(0L));

//        map.put("openRate",
//                totalMails == 0
//                        ? 0
//                        : (opens * 100.0) / totalMails);

        map.put("avgMailsPerUser",
                totalUsers == 0
                        ? 0
                        : (double) totalMails / totalUsers);

        return map;
    }
}
