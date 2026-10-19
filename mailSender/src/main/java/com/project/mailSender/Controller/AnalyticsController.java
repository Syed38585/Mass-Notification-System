package com.project.mailSender.Controller;


import com.project.mailSender.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    Repo userRepo;

    @Autowired
    MailLogRepo mailLogRepo;

    @Autowired
    AiLogRepo aiLogRepo;

    @Autowired
    EmailOpenLogRepo emailOpenLogRepo;

    @GetMapping("/users/gender")
    public Map<String, Long> genderStats() {

        long male = userRepo.findByGender("male").size();
        long female = userRepo.findByGender("female").size();

        Map<String, Long> map = new HashMap<>();
        map.put("male", male);
        map.put("female", female);

        return map;
    }

    @GetMapping("/users/count")
    public long totalUsers() {
        return userRepo.count();
    }

    @GetMapping("/users/male/count")
    public long maleUsers() {
        return userRepo.findByGender("male").size();
    }

    @GetMapping("/users/female/count")
    public long femaleUsers() {
        return userRepo.findByGender("female").size();
    }

    @GetMapping("/mails/count")
    public long totalMails() {
        return mailLogRepo.count();
    }

    @GetMapping("/ai/count")
    public long totalAi() {
        LocalDateTime startOfDay =
                LocalDateTime.now()
                        .toLocalDate()
                        .atStartOfDay();

        return aiLogRepo
                .findByRequestedAtAfter(startOfDay)
                .size();
    }

    @GetMapping("/mails/daily")
    public long mailsToday() {
        LocalDateTime startOfDay =
                LocalDateTime.now()
                        .toLocalDate()
                        .atStartOfDay();

        return mailLogRepo
                .findBySentAtAfter(startOfDay)
                .size();
    }


    @GetMapping("/average-time")
    public long getAverageSendTime() {
        return mailLogRepo.findAverageDuration();
    }

    @GetMapping("/last-duration")
    public long getLastDuration() {
        return mailLogRepo.findTopByOrderBySentAtDesc()
                .map(log -> log.getDurationMs())
                .orElse(0L);
    }

    @GetMapping("/email-opens")
    public List<Map<String, Object>> getEmailOpens() {

        List<Map<String, Object>> response = new ArrayList<>();

        emailOpenLogRepo.findAllByOrderByOpenedAtDesc()
                .forEach(log -> {

                    Map<String, Object> map = new HashMap<>();
                    map.put("email", log.getEmail());
                    map.put("openedAt", log.getOpenedAt());

                    response.add(map);
                });

        return response;
    }

    @GetMapping("/email-opens/count")
    public long totalOpens() {
        return emailOpenLogRepo.count();
    }

    @GetMapping("/open-rate")
    public double openRate() {

        long mails = mailLogRepo.count();

        if (mails == 0) {
            return 0;
        }

        return (emailOpenLogRepo.count() * 100.0) / mails;
    }

    @GetMapping("/avg-mails-per-user")
    public double avgMailsPerUser() {

        long users = userRepo.count();

        if (users == 0) {
            return 0;
        }

        return (double) mailLogRepo.count() / users;
    }

    @GetMapping("/dashboard")
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
        map.put("emailOpens", opens);

        map.put("avgSendTime",
                mailLogRepo.findAverageDuration());

        map.put("lastDuration",
                mailLogRepo.findTopByOrderBySentAtDesc()
                        .map(log -> log.getDurationMs())
                        .orElse(0L));

        map.put("openRate",
                totalMails == 0
                        ? 0
                        : (opens * 100.0) / totalMails);

        map.put("avgMailsPerUser",
                totalUsers == 0
                        ? 0
                        : (double) totalMails / totalUsers);

        return map;
    }
}