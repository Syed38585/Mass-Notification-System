package com.project.mailSender.Controller;

import com.project.mailSender.Model.EmailOpenLog;
import com.project.mailSender.Repository.EmailOpenLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;

@RestController
@RequestMapping("/api/track")
public class EmailTrackingController {

    @Autowired
    private EmailOpenLogRepo emailOpenLogRepo;

    @GetMapping("/open")
    public ResponseEntity<byte[]> trackOpen(
            @RequestParam String email,
            @RequestParam String trackingId
    ) {

        System.out.println("EMAIL OPENED: " + email);
        System.out.println("TRACKING ID: " + trackingId);

        EmailOpenLog log = new EmailOpenLog();
        log.setEmail(email);
        log.setOpenedAt(LocalDateTime.now());

        emailOpenLogRepo.save(log);

        byte[] pixel = Base64.getDecoder().decode(
                "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=="
        );

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .body(pixel);
    }
}