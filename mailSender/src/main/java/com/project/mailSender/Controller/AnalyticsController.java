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
    EmailOpenLogRepo  emailOpenLogRepo;

    @GetMapping("/users/gender")
    public Map<String, Long> genderStats(){

        long male = userRepo.findByGender("male").size();
        long female = userRepo.findByGender("female").size();

        Map<String, Long> map = new HashMap<>();
        map.put("male", male);
        map.put("female", female);

        return map;
    }

    @GetMapping("/mails/count")
    public long totalMails(){
        return mailLogRepo.count();
    }

    @GetMapping("/ai/count")
    public long totalAi(){
        return aiLogRepo.count();
    }

    @GetMapping("/mails/daily")
    public long mailsToday(){
        return mailLogRepo
                .findBySentAtAfter(LocalDateTime.now().minusDays(1))
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
}