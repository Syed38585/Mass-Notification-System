package com.project.mailSender.Controller;

import com.project.mailSender.Repository.AiLogRepo;
import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Repository.Repo;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final Repo userRepo;
    private final MailLogRepo mailLogRepo;
    private final AiLogRepo aiLogRepo;

    private static final ZoneId IST_ZONE =
            ZoneId.of("Asia/Kolkata");

    public AnalyticsController(
            Repo userRepo,
            MailLogRepo mailLogRepo,
            AiLogRepo aiLogRepo) {

        this.userRepo = userRepo;
        this.mailLogRepo = mailLogRepo;
        this.aiLogRepo = aiLogRepo;
    }

    // =====================================================
    // TODAY'S START TIME
    // =====================================================

    private LocalDateTime startOfToday() {

        return LocalDate.now(IST_ZONE).atStartOfDay();
    }

    // =====================================================
    // USER STATISTICS
    // =====================================================

    private long maleUserCount() {
        return userRepo.findByGender("male").size();
    }

    private long femaleUserCount() {
        return userRepo.findByGender("female").size();
    }

    @GetMapping("/users/gender")
    public Map<String, Long> genderStats() {

        Map<String, Long> result = new HashMap<>();

        result.put("male", maleUserCount());
        result.put("female", femaleUserCount());

        return result;
    }

    @GetMapping("/users/count")
    public long totalUsers() {
        return userRepo.count();
    }

    @GetMapping("/users/male/count")
    public long maleUsers() {
        return maleUserCount();
    }

    @GetMapping("/users/female/count")
    public long femaleUsers() {
        return femaleUserCount();
    }

    // =====================================================
    // MAIL STATISTICS
    // =====================================================

    @GetMapping("/mails/count")
    public long totalMails() {

        return mailLogRepo.count();
    }

    @GetMapping("/mails/daily")
    public long mailsToday() {

        return mailLogRepo
                .findBySentAtAfter(startOfToday())
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

    @GetMapping("/avg-mails-per-user")
    public double avgMailsPerUser() {

        long users = userRepo.count();

        if (users == 0) {
            return 0.0;
        }

        return (double) mailLogRepo.count() / users;
    }

    // =====================================================
    // AI STATISTICS
    // =====================================================

    @GetMapping("/ai/count")
    public long totalAiToday() {

        return aiLogRepo.countByRequestedAtAfter(
                startOfToday()
        );
    }

    // =====================================================
    // COMBINED DASHBOARD
    // =====================================================

    @GetMapping("/dashboard")
    public Map<String, Object> dashboardSummary() {

        long users = userRepo.count();

        long mails = mailLogRepo.count();

        long aiToday = totalAiToday();

        long mailsToday = mailsToday();

        Map<String, Object> result = new HashMap<>();

        result.put("totalUsers", users);

        result.put("maleUsers", maleUserCount());

        result.put("femaleUsers", femaleUserCount());

        result.put("totalMails", mails);

        result.put("mailsToday", mailsToday);

        result.put("aiRequests", aiToday);

        result.put(
                "avgSendTime",
                getAverageSendTime()
        );

        result.put(
                "lastDuration",
                getLastDuration()
        );

        result.put(
                "avgMailsPerUser",
                users == 0 ? 0.0 : (double) mails / users
        );

        return result;
    }
}