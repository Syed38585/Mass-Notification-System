package com.project.mailSender.Controller;

import com.project.mailSender.MailService.MailService;
import com.project.mailSender.Model.MailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@CrossOrigin(origins = "*")
public class MailController {

    @Autowired
    private MailService mailService;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @PostMapping(value = "/dispatch", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> dispatchMail(
            @ModelAttribute MailRequest mailRequest,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) List<Long> recipientIds,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile pdf) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (mailRequest.getSubject() == null
                    || mailRequest.getSubject().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email subject is required.");
                return ResponseEntity.badRequest().body(response);
            }

            if (mailRequest.getContent() == null
                    || mailRequest.getContent().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email content is required.");
                return ResponseEntity.badRequest().body(response);
            }

            // LocalDateTime.now(ZoneId) already returns LocalDateTime.
            // Do not append .toLocalDateTime() here.
            LocalDateTime nowIST = LocalDateTime.now(IST_ZONE);

            if (mailRequest.getSendAt() != null
                    && mailRequest.getSendAt().isBefore(nowIST)) {
                response.put("success", false);
                response.put("message", "Scheduled time must be in the future.");
                return ResponseEntity.badRequest().body(response);
            }

            if (recipientIds != null && !recipientIds.isEmpty()) {
                mailRequest.setRecipientIds(recipientIds);
            }

            byte[] imageBytes = null;
            String imageName = null;
            if (image != null && !image.isEmpty()) {
                imageBytes = image.getBytes();
                imageName = image.getOriginalFilename();
            }

            byte[] pdfBytes = null;
            String pdfName = null;
            if (pdf != null && !pdf.isEmpty()) {
                pdfBytes = pdf.getBytes();
                pdfName = pdf.getOriginalFilename();
            }

            mailService.processDispatch(
                    mailRequest,
                    gender,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

            response.put("success", true);
            if (mailRequest.getSendAt() != null) {
                response.put("message", "Email campaign scheduled successfully.");
                response.put("scheduledAt", mailRequest.getSendAt());
            } else {
                response.put("message", "Email campaign submitted successfully.");
            }

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process email campaign.");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}