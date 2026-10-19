package com.project.mailSender.Controller;

import com.project.mailSender.Model.EmailOpenLog;
import com.project.mailSender.Repository.EmailOpenLogRepo;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/track")
public class TrackingController {

    @Autowired
    private EmailOpenLogRepo repo;

    @GetMapping
    public ResponseEntity<byte[]> track(
            @RequestParam String email,
            @RequestParam String emailId,
            HttpServletRequest request
    ) {

        System.out.println("TRACKING API CALLED");

        // ✅ Avoid duplicate tracking
        if (repo.existsByEmailId(emailId)) {
            return getImageResponse();
        }

        // ✅ Save log
        EmailOpenLog log = new EmailOpenLog();
        log.setEmail(email);
        log.setEmailId(emailId);
        log.setOpenedAt(LocalDateTime.now());

        repo.save(log);

        System.out.println("📩 OPENED: " + email);

        return getImageResponse();
    }

    private ResponseEntity<byte[]> getImageResponse() {
        try {
            ClassPathResource imgFile = new ClassPathResource("static/logo.png");
            byte[] bytes = imgFile.getInputStream().readAllBytes();

            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();

            // fallback 1x1 pixel (never break email rendering)
            byte[] pixel = new byte[]{
                    71,73,70,56,57,97,1,0,1,0,-128,0,0,0,0,0,-1,-1,-1,
                    33,-7,4,1,0,0,0,0,44,0,0,0,0,1,0,1,0,0,2,2,68,1,0,59
            };

            return ResponseEntity.ok()
                    .header("Content-Type", "image/gif")
                    .body(pixel);
        }
    }

}